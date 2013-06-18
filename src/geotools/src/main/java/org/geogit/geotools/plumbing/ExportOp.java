/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DepthTreeIterator;
import org.geogit.api.plumbing.diff.DepthTreeIterator.Strategy;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.Hints;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Internal operation for creating a FeatureCollection from a tree content.
 * 
 */
public class ExportOp extends AbstractGeoGitOp<SimpleFeatureStore> {

    private String path;

    private Supplier<SimpleFeatureStore> featureStore;

    private ObjectDatabase database;

    private Function<Feature, Optional<Feature>> function = new Function<Feature, Optional<Feature>>() {

        @Override
        @Nullable
        public Optional<Feature> apply(@Nullable Feature feature) {
            return Optional.fromNullable(feature);
        }

    };

    private ObjectId featureTypeId;

    private boolean forceExportDefaultFeatureType;

    private boolean alter;

    /**
     * Constructs a new export operation.
     */
    @Inject
    public ExportOp(StagingDatabase database) {
        this.database = database;
    }

    /**
     * Executes the export operation using the parameters that have been specified.
     * 
     * @return a FeatureCollection with the specified features
     */
    @SuppressWarnings("deprecation")
    @Override
    public SimpleFeatureStore call() {

        SimpleFeatureStore fs;
        try {
            fs = featureStore.get();
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.CANNOT_CREATE_FEATURESTORE);
        }
        if (fs == null) {
            throw new GeoToolsOpException(StatusCode.CANNOT_CREATE_FEATURESTORE);
        }

        final String refspec;
        if (path.contains(":")) {
            refspec = path;
        } else {
            refspec = "WORK_HEAD:" + path;
        }
        final String treePath = refspec.substring(refspec.indexOf(':') + 1);

        Optional<ObjectId> tree = command(ResolveTreeish.class).setTreeish(
                refspec.substring(0, refspec.indexOf(':'))).call();

        Preconditions.checkArgument(tree.isPresent(), "Invalid tree spec: %s",
                refspec.substring(0, refspec.indexOf(':')));

        Optional<RevTree> revTree = command(RevObjectParse.class).setObjectId(tree.get()).call(
                RevTree.class);

        Preconditions.checkArgument(revTree.isPresent(), "Tree ref spec could not be resolved.");

        Optional<NodeRef> typeTreeRef = command(FindTreeChild.class).setIndex(true)
                .setParent(revTree.get()).setChildPath(treePath).call();

        ObjectId parentMetadataId = null;
        if (typeTreeRef.isPresent()) {
            parentMetadataId = typeTreeRef.get().getMetadataId();
        }

        if (forceExportDefaultFeatureType || (alter && featureTypeId == null)) {
            featureTypeId = parentMetadataId;
        }

        Optional<RevObject> revObject = command(RevObjectParse.class).setRefSpec(refspec).call(
                RevObject.class);

        Preconditions.checkArgument(revObject.isPresent(), "Invalid reference: %s", refspec);
        Preconditions.checkArgument(revObject.get().getType() == TYPE.TREE,
                "%s did not resolve to a tree", refspec);

        DepthTreeIterator iter = new DepthTreeIterator(treePath, parentMetadataId,
                (RevTree) revObject.get(), database, Strategy.FEATURES_ONLY);

        // Create a FeatureCollection

        getProgressListener().started();
        getProgressListener().setDescription("Exporting " + path + "... ");
        DefaultFeatureCollection features = new DefaultFeatureCollection();

        boolean featureTypeWasSpecified = featureTypeId != null;
        featureTypeId = featureTypeId == null ? parentMetadataId : featureTypeId;
        FeatureBuilder featureBuilder = null;
        // FeatureType featureType = null;
        int i = 1;
        while (iter.hasNext()) {
            NodeRef nodeRef = iter.next();
            if (nodeRef.getType() == TYPE.FEATURE) {
                RevFeature revFeature = command(RevObjectParse.class)
                        .setObjectId(nodeRef.objectId()).call(RevFeature.class).get();
                if (!nodeRef.getMetadataId().equals(featureTypeId)) {
                    // we skip features with a different feature type, but only if a featuretype was
                    // specified. If alter is used, then we convert them
                    if (!featureTypeWasSpecified) {
                        throw new GeoToolsOpException(StatusCode.MIXED_FEATURE_TYPES);
                    } else if (alter) {
                        RevFeatureType revFeatureType = command(RevObjectParse.class)
                                .setObjectId(featureTypeId).call(RevFeatureType.class).get();
                        // featureType = revFeatureType.type();
                        featureBuilder = new FeatureBuilder(revFeatureType);
                        revFeature = new RevFeatureBuilder().build(alter(nodeRef, revFeatureType));
                    } else {
                        continue;
                    }
                }
                if (featureBuilder == null) {
                    RevFeatureType revFeatureType = command(RevObjectParse.class)
                            .setObjectId(featureTypeId).call(RevFeatureType.class).get();
                    // featureType = revFeatureType.type();
                    featureBuilder = new FeatureBuilder(revFeatureType);
                }
                Feature feature = featureBuilder.build(nodeRef.getNode().getName(), revFeature);
                feature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                Optional<Feature> validFeature = function.apply(feature);
                if (validFeature.isPresent()) {
                    features.add((SimpleFeature) validFeature.get());
                }
                getProgressListener().progress((i * 100.f) / revTree.get().size());
                i++;
            }
        }

        if (featureBuilder == null) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_FEATURES);
        }

        // add the feature collection to the feature store
        final Transaction transaction = new DefaultTransaction("create");
        try {
            fs.setTransaction(transaction);
            try {
                fs.addFeatures(features);
                transaction.commit();
            } catch (final Exception e) {
                transaction.rollback();
                throw new GeoToolsOpException(StatusCode.UNABLE_TO_ADD);
            } finally {
                features = null;
                transaction.close();
            }
        } catch (IOException e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_ADD);
        }

        getProgressListener().complete();

        return fs;

    }

    /**
     * Translates a feature pointed by a node from its original feature type to a given one, using
     * values from those attributes that exist in both original and destination feature type. New
     * attributes are populated with null values
     * 
     * @param node The node that points to the feature. No checking is performed to ensure the node
     *        points to a feature instead of other type
     * @param featureType the destination feature type
     * @return a feature with the passed feature type and data taken from the input feature
     */
    private Feature alter(NodeRef node, RevFeatureType featureType) {
        RevFeature oldFeature = command(RevObjectParse.class).setObjectId(node.objectId())
                .call(RevFeature.class).get();
        RevFeatureType oldFeatureType;
        oldFeatureType = command(RevObjectParse.class).setObjectId(node.getMetadataId())
                .call(RevFeatureType.class).get();
        ImmutableList<PropertyDescriptor> oldAttributes = oldFeatureType.sortedDescriptors();
        ImmutableList<PropertyDescriptor> newAttributes = featureType.sortedDescriptors();
        ImmutableList<Optional<Object>> oldValues = oldFeature.getValues();
        List<Optional<Object>> newValues = Lists.newArrayList();
        for (int i = 0; i < newAttributes.size(); i++) {
            int idx = oldAttributes.indexOf(newAttributes.get(i));
            if (idx != -1) {
                Optional<Object> oldValue = oldValues.get(idx);
                newValues.add(oldValue);
            } else {
                newValues.add(Optional.absent());
            }
        }
        RevFeature newFeature = RevFeature.build(ImmutableList.copyOf(newValues));
        FeatureBuilder featureBuilder = new FeatureBuilder(featureType);
        Feature feature = featureBuilder.build(node.name(), newFeature);
        return feature;
    }

    /**
     * 
     * @param featureStore a supplier that resolves to the feature store to use for exporting
     * @return
     */
    public ExportOp setFeatureStore(Supplier<SimpleFeatureStore> featureStore) {
        this.featureStore = featureStore;
        return this;
    }

    /**
     * 
     * @param featureStore the feature store to use for exporting
     * @return
     */
    public ExportOp setFeatureStore(SimpleFeatureStore featureStore) {
        this.featureStore = Suppliers.ofInstance(featureStore);
        return this;
    }

    /**
     * @param path the path to export Supports the [refspec]:[path] syntax
     * @return {@code this}
     */
    public ExportOp setPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * @param featureType the Id of the featureType of the features to export. If this is provided,
     *        only features with this feature type will be exported. If this is not provided and the
     *        path to export contains features with several different feature types, an exception
     *        will be throw to warn about it
     * @return {@code this}
     */
    public ExportOp setFeatureTypeId(ObjectId featureTypeId) {
        this.featureTypeId = featureTypeId;
        return this;
    }

    /**
     * If set to true, all features will be exported, even if they do not have
     * 
     * @param alter whther to alter features before exporting, if they do not have the output
     *        feature type
     * @return
     */
    public ExportOp setAlter(boolean alter) {
        this.alter = alter;
        return this;
    }

    /**
     * Calling this method causes the export operation to be performed in case the features in the
     * specified path have several different feature types, without throwing an exception. Only
     * features with the default feature type of the path will be exported. This has the same effect
     * as calling the setFeatureType method with the Id of the default feature type of the path to
     * export
     * 
     * If both this method and setFeatureId are used, this one will have priority
     * 
     * @return {@code this}
     */
    public ExportOp exportDefaultFeatureType() {
        this.forceExportDefaultFeatureType = true;
        return this;
    }

    /**
     * Sets the function to use for creating a valid Feature that has the FeatureType of the output
     * FeatureStore, based on the actual FeatureType of the Features to export.
     * 
     * The Export operation assumes that the feature returned by this function are valid to be added
     * to the current FeatureSource, and, therefore, performs no checking of FeatureType matching.
     * It is up to the user performing the export to ensure that the function actually generates
     * valid features for the current FeatureStore.
     * 
     * If no function is explicitly set, and identity function is used, and Features are not
     * converted.
     * 
     * This function can be used as a filter as well. If the returned object is Optional.absent, no
     * feature will be added
     * 
     * @param function
     * @return {@code this}
     */
    public ExportOp setFeatureTypeConversionFunction(Function<Feature, Optional<Feature>> function) {
        this.function = function;
        return this;
    }
}
