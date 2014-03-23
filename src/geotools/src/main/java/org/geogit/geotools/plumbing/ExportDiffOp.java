/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseFeatureCollection;
import org.geotools.feature.collection.DelegateFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.geogit.api.ProgressListener;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.inject.Inject;

/**
 * Internal operation for creating a FeatureCollection from a tree content.
 * 
 */

public class ExportDiffOp extends AbstractGeoGitOp<SimpleFeatureStore> {

    private static final Function<Feature, Optional<Feature>> IDENTITY = new Function<Feature, Optional<Feature>>() {

        @Override
        @Nullable
        public Optional<Feature> apply(@Nullable Feature feature) {
            return Optional.fromNullable(feature);
        }

    };

    private String path;

    private Supplier<SimpleFeatureStore> targetStoreProvider;

    private StagingDatabase database;

    private Function<Feature, Optional<Feature>> function = IDENTITY;

    private boolean transactional;

    private boolean old;

    private String newRef;

    private String oldRef;

    /**
     * Constructs a new export operation.
     */
    @Inject
    public ExportDiffOp(StagingDatabase database) {
        this.database = database;
        this.transactional = true;
    }

    /**
     * Executes the export operation using the parameters that have been specified.
     * 
     * @return a FeatureCollection with the specified features
     */
    @SuppressWarnings("deprecation")
    @Override
    public SimpleFeatureStore call() {

        final SimpleFeatureStore targetStore = getTargetStore();

        final String refspec = old ? oldRef : newRef;
        final RevTree rootTree = resolveRootTree(refspec);
        final NodeRef typeTreeRef = resolTypeTreeRef(refspec, path, rootTree);
        final ObjectId defaultMetadataId = typeTreeRef.getMetadataId();

        final ProgressListener progressListener = getProgressListener();

        progressListener.started();
        progressListener.setDescription("Exporting diffs for path '" + path + "'... ");

        FeatureCollection<SimpleFeatureType, SimpleFeature> asFeatureCollection = new BaseFeatureCollection<SimpleFeatureType, SimpleFeature>() {

            @Override
            public FeatureIterator<SimpleFeature> features() {

                Iterator<DiffEntry> diffs = command(DiffOp.class).setOldVersion(oldRef)
                        .setNewVersion(newRef).setFilter(path).call();

                final Iterator<SimpleFeature> plainFeatures = getFeatures(diffs, old, database,
                        defaultMetadataId, progressListener);

                return new DelegateFeatureIterator<SimpleFeature>(plainFeatures);
            }
        };

        // add the feature collection to the feature store
        final Transaction transaction;
        if (transactional) {
            transaction = new DefaultTransaction("create");
        } else {
            transaction = Transaction.AUTO_COMMIT;
        }
        try {
            targetStore.setTransaction(transaction);
            try {
                targetStore.addFeatures(asFeatureCollection);
                transaction.commit();
            } catch (final Exception e) {
                if (transactional) {
                    transaction.rollback();
                }
                Throwables.propagateIfInstanceOf(e, GeoToolsOpException.class);
                throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_ADD);
            } finally {
                transaction.close();
            }
        } catch (IOException e) {
            throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_ADD);
        }

        progressListener.complete();

        return targetStore;

    }

    private static Iterator<SimpleFeature> getFeatures(Iterator<DiffEntry> diffs,
            final boolean old, final ObjectDatabase database, final ObjectId metadataId,
            final ProgressListener progressListener) {

        final SimpleFeatureType featureType = addFidAttribute(database.getFeatureType(metadataId));
        final RevFeatureType revFeatureType = RevFeatureType.build(featureType);
        final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);

        Function<DiffEntry, SimpleFeature> asFeature = new Function<DiffEntry, SimpleFeature>() {

            @Override
            @Nullable
            public SimpleFeature apply(final DiffEntry input) {
                NodeRef nodeRef = old ? input.getOldObject() : input.getNewObject();
                if (nodeRef == null) {
                    return null;
                }
                final RevFeature revFeature = database.getFeature(nodeRef.objectId());
                ImmutableList<Optional<Object>> values = revFeature.getValues();
                for (int i = 0; i < values.size(); i++) {
                    String name = featureType.getDescriptor(i + 1).getLocalName();
                    Object value = values.get(i).orNull();
                    featureBuilder.set(name, value);
                }
                featureBuilder.set("geogit_fid", nodeRef.name());
                Feature feature = featureBuilder.buildFeature(nodeRef.name());
                feature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                feature.getUserData().put(RevFeature.class, revFeature);
                feature.getUserData().put(RevFeatureType.class, revFeatureType);

                if (feature instanceof SimpleFeature) {
                    return (SimpleFeature) feature;
                }
                return null;
            }

        };

        Iterator<SimpleFeature> asFeatures = Iterators.transform(diffs, asFeature);

        UnmodifiableIterator<SimpleFeature> filterNulls = Iterators.filter(asFeatures,
                Predicates.notNull());

        return filterNulls;
    }

    private static SimpleFeatureType addFidAttribute(RevFeatureType revFType) {
        SimpleFeatureType featureType = (SimpleFeatureType) revFType.type();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geogit_fid", String.class);
        for (AttributeDescriptor descriptor : featureType.getAttributeDescriptors()) {
            builder.add(descriptor);
        }
        builder.setName(featureType.getName());
        builder.setCRS(featureType.getCoordinateReferenceSystem());
        featureType = builder.buildFeatureType();
        return featureType;
    }

    private NodeRef resolTypeTreeRef(final String refspec, final String treePath,
            final RevTree rootTree) {
        Optional<NodeRef> typeTreeRef = command(FindTreeChild.class).setIndex(true)
                .setParent(rootTree).setChildPath(treePath).call();
        checkArgument(typeTreeRef.isPresent(), "Type tree %s does not exist", refspec);
        checkArgument(TYPE.TREE.equals(typeTreeRef.get().getType()),
                "%s did not resolve to a tree", refspec);
        return typeTreeRef.get();
    }

    private RevTree resolveRootTree(final String refspec) {
        Optional<ObjectId> rootTreeId = command(ResolveTreeish.class).setTreeish(refspec).call();

        checkArgument(rootTreeId.isPresent(), "Invalid tree spec: %s", refspec);

        RevTree rootTree = database.getTree(rootTreeId.get());
        return rootTree;
    }

    private SimpleFeatureStore getTargetStore() {
        SimpleFeatureStore targetStore;
        try {
            targetStore = targetStoreProvider.get();
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.CANNOT_CREATE_FEATURESTORE);
        }
        if (targetStore == null) {
            throw new GeoToolsOpException(StatusCode.CANNOT_CREATE_FEATURESTORE);
        }
        return targetStore;
    }

    /**
     * 
     * @param featureStore a supplier that resolves to the feature store to use for exporting
     * @return
     */
    public ExportDiffOp setFeatureStore(Supplier<SimpleFeatureStore> featureStore) {
        this.targetStoreProvider = featureStore;
        return this;
    }

    /**
     * 
     * @param featureStore the feature store to use for exporting The schema of the feature store
     *        must be equal to the one of the layer whose diffs are to be exported, plus an
     *        additional "geogit_fid" field of type String, which is used to include the id of each
     *        feature.
     * 
     * @return
     */
    public ExportDiffOp setFeatureStore(SimpleFeatureStore featureStore) {
        this.targetStoreProvider = Suppliers.ofInstance(featureStore);
        return this;
    }

    /**
     * @param path the path to export
     * @return {@code this}
     */
    public ExportDiffOp setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * @param transactional whether to use a geotools transaction for the operation, defaults to
     *        {@code true}
     */
    public ExportDiffOp setTransactional(boolean transactional) {
        this.transactional = transactional;
        return this;
    }

    public ExportDiffOp setNewRef(String newRef) {
        this.newRef = newRef;
        return this;
    }

    public ExportDiffOp setOldRef(String oldRef) {
        this.oldRef = oldRef;
        return this;
    }

    public ExportDiffOp setUseOld(boolean old) {
        this.old = old;
        return this;
    }
}
