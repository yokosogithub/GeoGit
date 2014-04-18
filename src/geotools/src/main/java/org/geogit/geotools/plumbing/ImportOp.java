<<<<<<< .merge_file_RuT4AB
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.data.ForwardingFeatureCollection;
import org.geogit.api.data.ForwardingFeatureIterator;
import org.geogit.api.data.ForwardingFeatureSource;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.DecoratingFeature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.identity.FeatureId;
import org.geogit.api.ProgressListener;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * Internal operation for importing tables from a GeoTools {@link DataStore}.
 * 
 * @see DataStore
 */

public class ImportOp extends AbstractGeoGitOp<RevTree> {

    private boolean all = false;

    private String table = null;

    /**
     * The path to import the data into
     */
    private String destPath;

    /**
     * The name to use for the geometry descriptor, replacing the default one
     */
    private String geomName;

    /**
     * The name of the attribute to use for defining feature id's
     */
    private String fidAttribute;

    private DataStore dataStore;

    /**
     * Whether to remove previous objects in the destination path, in case they exist
     * 
     */
    private boolean overwrite = true;

    /**
     * If true, it does not overwrite, and modifies the existing features to have the same feature
     * type as the imported table
     */
    private boolean alter;

    /**
     * Constructs a new {@code ImportOp} operation.
     */
    @Inject
    public ImportOp() {
    }

    /**
     * Executes the import operation using the parameters that have been specified. Features will be
     * added to the working tree, and a new working tree will be constructed. Either {@code all} or
     * {@code table}, but not both, must be set prior to the import process.
     * 
     * @return RevTree the new working tree
     */
    @SuppressWarnings("deprecation")
    @Override
    public RevTree call() {

        // check preconditions and get the actual list of type names to import
        final String[] typeNames = checkPreconditions();

        ProgressListener progressListener = getProgressListener();
        progressListener.started();

        // use a local variable not to alter the command's state
        boolean overwrite = this.overwrite;
        if (alter) {
            overwrite = false;
        }

        final WorkingTree workTree = getWorkTree();

        final boolean destPathProvided = destPath != null;
        if (destPathProvided && overwrite) {
            // we delete the previous tree to honor the overwrite setting, but then turn it
            // to false. Otherwise, each table imported will overwrite the previous ones and
            // only the last one will be imported.
            try {
                workTree.delete(destPath);
            } catch (Exception e) {
                throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_INSERT);
            }
            overwrite = false;
        }

        int tableCount = 0;

        for (String typeName : typeNames) {
            {
                tableCount++;
                String tableName = String.format("%-16s", typeName);
                if (typeName.length() > 16) {
                    tableName = tableName.substring(0, 13) + "...";
                }
                progressListener.setDescription("Importing " + tableName + " (" + tableCount + "/"
                        + typeNames.length + ")... ");
            }

            FeatureSource featureSource = getFeatureSource(typeName);
            SimpleFeatureType featureType = (SimpleFeatureType) featureSource.getSchema();

            final String fidPrefix = featureType.getTypeName() + ".";

            String path;
            if (destPath == null) {
                path = featureType.getTypeName();
            } else {
                NodeRef.checkValidPath(destPath);
                path = destPath;
                featureType = createForceFeatureType(featureType, path);
            }

            featureType = overrideGeometryName(featureType);

            featureSource = new ForceTypeAndFidFeatureSource<FeatureType, Feature>(featureSource,
                    featureType, fidPrefix);

            ProgressListener taskProgress = subProgress(100.f / typeNames.length);
            if (overwrite) {
                try {
                    workTree.delete(path);
                    workTree.createTypeTree(path, featureType);
                } catch (Exception e) {
                    throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_INSERT);
                }
            }

            if (alter) {
                // first we modify the feature type and the existing features, if needed
                workTree.updateTypeTree(path, featureType);
                Iterator<Feature> transformedIterator = transformFeatures(featureType, path);
                try {
                    final Integer collectionSize = collectionSize(featureSource);
                    workTree.insert(path, transformedIterator, taskProgress, null, collectionSize);
                } catch (Exception e) {
                    throw new GeoToolsOpException(StatusCode.UNABLE_TO_INSERT);
                }
            }

            try {
                insert(workTree, path, featureSource, taskProgress);
            } catch (Exception e) {
                throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_INSERT);
            }
        }

        progressListener.setProgress(100.f);
        progressListener.complete();
        return workTree.getTree();
    }

    private SimpleFeatureType overrideGeometryName(SimpleFeatureType featureType) {

        if (geomName == null) {
            return featureType;
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        List<AttributeDescriptor> newAttributes = Lists.newArrayList();

        String oldGeomName = featureType.getGeometryDescriptor().getName().getLocalPart();
        Collection<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();

        for (AttributeDescriptor descriptor : descriptors) {
            String name = descriptor.getName().getLocalPart();
            Preconditions.checkArgument(!name.equals(geomName),
                    "The provided geom name is already in use by another attribute");
            if (name.equals(oldGeomName)) {
                AttributeDescriptorImpl newDescriptor = new AttributeDescriptorImpl(
                        descriptor.getType(), new NameImpl(geomName), descriptor.getMinOccurs(),
                        descriptor.getMaxOccurs(), descriptor.isNillable(),
                        descriptor.getDefaultValue());
                newAttributes.add(newDescriptor);
            } else {
                newAttributes.add(descriptor);
            }
        }

        builder.setAttributes(newAttributes);
        builder.setName(featureType.getName());
        builder.setCRS(featureType.getCoordinateReferenceSystem());
        featureType = builder.buildFeatureType();
        return featureType;

    }

    private SimpleFeatureType createForceFeatureType(SimpleFeatureType featureType, String path) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setAttributes(featureType.getAttributeDescriptors());
        builder.setName(new NameImpl(featureType.getName().getNamespaceURI(), path));
        builder.setCRS(featureType.getCoordinateReferenceSystem());

        featureType = builder.buildFeatureType();
        return featureType;
    }

    private Iterator<Feature> transformFeatures(SimpleFeatureType featureType, String path) {
        String refspec = Ref.WORK_HEAD + ":" + path;
        Iterator<NodeRef> oldFeatures = command(LsTreeOp.class).setReference(refspec)
                .setStrategy(Strategy.FEATURES_ONLY).call();

        RevFeatureType revFeatureType = RevFeatureType.build(featureType);
        Iterator<Feature> transformedIterator = transformIterator(oldFeatures, revFeatureType);
        return transformedIterator;
    }

    private Integer collectionSize(FeatureSource featureSource) {
        final Integer collectionSize;
        {
            int fastCount;
            try {
                fastCount = featureSource.getCount(Query.ALL);
            } catch (IOException e) {
                throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_GET_FEATURES);
            }
            collectionSize = -1 == fastCount ? null : Integer.valueOf(fastCount);
        }
        return collectionSize;
    }

    private String[] checkPreconditions() {
        if (dataStore == null) {
            throw new GeoToolsOpException(StatusCode.DATASTORE_NOT_DEFINED);
        }

        if ((table == null || table.isEmpty()) && !(all)) {
            throw new GeoToolsOpException(StatusCode.TABLE_NOT_DEFINED);
        }

        if (table != null && !table.isEmpty() && all) {
            throw new GeoToolsOpException(StatusCode.ALL_AND_TABLE_DEFINED);
        }
        String[] typeNames;
        if (all) {
            try {
                typeNames = dataStore.getTypeNames();
            } catch (Exception e) {
                throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_NAMES);
            }
            if (typeNames.length == 0) {
                throw new GeoToolsOpException(StatusCode.NO_FEATURES_FOUND);
            }
        } else {
            SimpleFeatureType schema;
            try {
                schema = dataStore.getSchema(table);
            } catch (IOException e) {
                throw new GeoToolsOpException(StatusCode.TABLE_NOT_FOUND);
            }
            Preconditions.checkNotNull(schema);
            typeNames = new String[] { table };
        }

        if (typeNames.length > 1 && alter && all) {
            throw new GeoToolsOpException(StatusCode.ALTER_AND_ALL_DEFINED);
        }
        return typeNames;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FeatureSource getFeatureSource(String typeName) {

        FeatureSource featureSource;
        try {
            featureSource = dataStore.getFeatureSource(typeName);
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_FEATURES);
        }

        return new ForwardingFeatureSource(featureSource) {

            @Override
            public FeatureCollection getFeatures(Query query) throws IOException {

                final FeatureCollection features = super.getFeatures(query);
                return new ForwardingFeatureCollection(features) {

                    @Override
                    public FeatureIterator features() {

                        final FeatureType featureType = getSchema();
                        final String fidPrefix = featureType.getName().getLocalPart() + ".";

                        FeatureIterator iterator = delegate.features();

                        return new FidAndFtReplacerIterator(iterator, fidAttribute, fidPrefix,
                                (SimpleFeatureType) featureType);
                    }
                };
            }
        };
    }

    /**
     * Replaces the default geotools fid with the string representation of the value of an
     * attribute.
     * 
     * If the specified attribute is null, does not exist or the value is null, an fid is created by
     * taking the default fid and removing the specified fidPrefix prefix from it.
     * 
     * It also replaces the feature type. This is used to avoid identical feature types (in terms of
     * attributes) coming from different data sources (such as to shapefiles with different names)
     * being considered different for having a different name. It is used in this importer class to
     * decorate the name of the feature type when importing into a given tree, using the name of the
     * tree.
     * 
     * The passed feature type should have the same attribute descriptions as the one to replace,
     * but no checking is performed to ensure that
     * 
     */
    private static class FidAndFtReplacerIterator extends ForwardingFeatureIterator<SimpleFeature> {

        private final String fidPrefix;

        private String attributeName;

        private SimpleFeatureType featureType;

        public FidAndFtReplacerIterator(FeatureIterator iterator, final String attributeName,
                String fidPrefix, SimpleFeatureType featureType) {
            super(iterator);
            this.attributeName = attributeName;
            this.fidPrefix = fidPrefix;
            this.featureType = featureType;

        }

        @Override
        public SimpleFeature next() {
            SimpleFeature next = super.next();
            if (attributeName == null) {
                String fid = next.getID();
                if (fid.startsWith(fidPrefix)) {
                    fid = fid.substring(fidPrefix.length());
                }
                return new FidAndFtOverrideFeature(next, fid, featureType);
            } else {
                Object value = next.getAttribute(attributeName);
                Preconditions.checkNotNull(value);
                return new FidAndFtOverrideFeature(next, value.toString(), featureType);
            }
        }
    }

    private void insert(final WorkingTree workTree, final String path,
            final FeatureSource featureSource, final ProgressListener taskProgress) {

        final Query query = new Query();
        CoordinateSequenceFactory coordSeq = new PackedCoordinateSequenceFactory();
        query.getHints().add(new Hints(Hints.JTS_COORDINATE_SEQUENCE_FACTORY, coordSeq));
        workTree.insert(path, featureSource, query, taskProgress);

    }

    private Iterator<Feature> transformIterator(Iterator<NodeRef> nodeIterator,
            final RevFeatureType newFeatureType) {

        Iterator<Feature> iterator = Iterators.transform(nodeIterator,
                new Function<NodeRef, Feature>() {
                    @Override
                    public Feature apply(NodeRef node) {
                        return alter(node, newFeatureType);
                    }

                });

        return iterator;

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
     * @param all if this is set, all tables from the data store will be imported
     * @return {@code this}
     */
    public ImportOp setAll(boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @param table if this is set, only the specified table will be imported from the data store
     * @return {@code this}
     */
    public ImportOp setTable(String table) {
        this.table = table;
        return this;
    }

    /**
     * 
     * @param overwrite If this is true, existing features will be overwritten in case they exist
     *        and have the same path and Id than the features to import. If this is false, existing
     *        features will not be overwritten, and a safe import is performed, where only those
     *        features that do not already exists are added
     * @return {@code this}
     */
    public ImportOp setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    /**
     * 
     * @param the attribute to use to create the feature id, if the default.
     */
    public ImportOp setFidAttribute(String attribute) {
        this.fidAttribute = attribute;
        return this;
    }

    /**
     * @param force if true, it will change the default feature type of the tree we are importing
     *        into and change all features under that tree to have that same feature type
     * @return {@code this}
     */
    public ImportOp setAlter(boolean alter) {
        this.alter = alter;
        return this;
    }

    /**
     * 
     * @param destPath the path to import to to. If not provided, it will be taken from the feature
     *        type of the table to import.
     * @return {@code this}
     */
    public ImportOp setDestinationPath(@Nullable String destPath) {
        Preconditions.checkArgument(destPath == null || !destPath.isEmpty());
        this.destPath = destPath;
        return this;
    }

    /**
     * @param dataStore the data store to use for the import process
     * @return {@code this}
     */
    public ImportOp setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return this;
    }

    private static final class FidAndFtOverrideFeature extends DecoratingFeature {

        private String fid;

        private SimpleFeatureType featureType;

        public FidAndFtOverrideFeature(SimpleFeature delegate, String fid,
                SimpleFeatureType featureType) {
            super(delegate);
            this.fid = fid;
            this.featureType = featureType;
        }

        @Override
        public SimpleFeatureType getType() {
            return featureType;
        }

        @Override
        public String getID() {
            return fid;
        }

        @Override
        public FeatureId getIdentifier() {
            return new FeatureIdImpl(fid);
        }
    }

    /**
     * Sets the name to use for the geometry descriptor. If not provided, the geometry name from
     * the source schema will be used.
     * 
     * @param geomName
     */
    public ImportOp setGeometryNameOverride(String geomName) {
        this.geomName = geomName;
        return this;

    }
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.ProgressListener;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.data.ForwardingFeatureCollection;
import org.geogit.api.data.ForwardingFeatureIterator;
import org.geogit.api.data.ForwardingFeatureSource;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.DecoratingFeature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.identity.FeatureId;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * Internal operation for importing tables from a GeoTools {@link DataStore}.
 * 
 * @see DataStore
 */

public class ImportOp extends AbstractGeoGitOp<RevTree> {

    private boolean all = false;

    private String table = null;

    /**
     * The path to import the data into
     */
    private String destPath;

    /**
     * The name to use for the geometry descriptor, replacing the default one
     */
    private String geomName;

    /**
     * The name of the attribute to use for defining feature id's
     */
    private String fidAttribute;

    private DataStore dataStore;

    /**
     * Whether to remove previous objects in the destination path, in case they exist
     * 
     */
    private boolean overwrite = true;

    /**
     * If true, it does not overwrite, and modifies the existing features to have the same feature
     * type as the imported table
     */
    private boolean alter;

    private boolean usePaging = true;

    /**
     * Constructs a new {@code ImportOp} operation.
     */
    @Inject
    public ImportOp() {
    }

    /**
     * Executes the import operation using the parameters that have been specified. Features will be
     * added to the working tree, and a new working tree will be constructed. Either {@code all} or
     * {@code table}, but not both, must be set prior to the import process.
     * 
     * @return RevTree the new working tree
     */
    @Override
    public RevTree call() {

        // check preconditions and get the actual list of type names to import
        final String[] typeNames = checkPreconditions();

        ProgressListener progressListener = getProgressListener();
        progressListener.started();

        // use a local variable not to alter the command's state
        boolean overwrite = this.overwrite;
        if (alter) {
            overwrite = false;
        }

        final WorkingTree workTree = getWorkTree();

        final boolean destPathProvided = destPath != null;
        if (destPathProvided && overwrite) {
            // we delete the previous tree to honor the overwrite setting, but then turn it
            // to false. Otherwise, each table imported will overwrite the previous ones and
            // only the last one will be imported.
            try {
                workTree.delete(destPath);
            } catch (Exception e) {
                throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_INSERT);
            }
            overwrite = false;
        }

        int tableCount = 0;

        for (String typeName : typeNames) {
            {
                tableCount++;
                String tableName = String.format("%-16s", typeName);
                if (typeName.length() > 16) {
                    tableName = tableName.substring(0, 13) + "...";
                }
                progressListener.setDescription("Importing " + tableName + " (" + tableCount + "/"
                        + typeNames.length + ")... ");
            }

            FeatureSource featureSource = getFeatureSource(typeName);
            SimpleFeatureType featureType = (SimpleFeatureType) featureSource.getSchema();

            final String fidPrefix = featureType.getTypeName() + ".";

            String path;
            if (destPath == null) {
                path = featureType.getTypeName();
            } else {
                NodeRef.checkValidPath(destPath);
                path = destPath;
                featureType = createForceFeatureType(featureType, path);
            }

            featureType = overrideGeometryName(featureType);

            featureSource = new ForceTypeAndFidFeatureSource<FeatureType, Feature>(featureSource,
                    featureType, fidPrefix);
            if (!usePaging) {
                ((ForceTypeAndFidFeatureSource) featureSource).setForbidSorting(true);
            }

            ProgressListener taskProgress = subProgress(100.f / typeNames.length);
            if (overwrite) {
                try {
                    workTree.delete(path);
                    workTree.createTypeTree(path, featureType);
                } catch (Exception e) {
                    throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_INSERT);
                }
            }

            if (alter) {
                // first we modify the feature type and the existing features, if needed
                workTree.updateTypeTree(path, featureType);
                Iterator<Feature> transformedIterator = transformFeatures(featureType, path);
                try {
                    final Integer collectionSize = collectionSize(featureSource);
                    workTree.insert(path, transformedIterator, taskProgress, null, collectionSize);
                } catch (Exception e) {
                    throw new GeoToolsOpException(StatusCode.UNABLE_TO_INSERT);
                }
            }

            try {
                insert(workTree, path, featureSource, taskProgress);
            } catch (Exception e) {
                throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_INSERT);
            }
        }

        progressListener.setProgress(100.f);
        progressListener.complete();
        return workTree.getTree();
    }

    private SimpleFeatureType overrideGeometryName(SimpleFeatureType featureType) {

        if (geomName == null) {
            return featureType;
        }

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        List<AttributeDescriptor> newAttributes = Lists.newArrayList();

        String oldGeomName = featureType.getGeometryDescriptor().getName().getLocalPart();
        Collection<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();

        for (AttributeDescriptor descriptor : descriptors) {
            String name = descriptor.getName().getLocalPart();
            Preconditions.checkArgument(!name.equals(geomName),
                    "The provided geom name is already in use by another attribute");
            if (name.equals(oldGeomName)) {
                AttributeDescriptorImpl newDescriptor = new AttributeDescriptorImpl(
                        descriptor.getType(), new NameImpl(geomName), descriptor.getMinOccurs(),
                        descriptor.getMaxOccurs(), descriptor.isNillable(),
                        descriptor.getDefaultValue());
                newAttributes.add(newDescriptor);
            } else {
                newAttributes.add(descriptor);
            }
        }

        builder.setAttributes(newAttributes);
        builder.setName(featureType.getName());
        builder.setCRS(featureType.getCoordinateReferenceSystem());
        featureType = builder.buildFeatureType();
        return featureType;

    }

    private SimpleFeatureType createForceFeatureType(SimpleFeatureType featureType, String path) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setAttributes(featureType.getAttributeDescriptors());
        builder.setName(new NameImpl(featureType.getName().getNamespaceURI(), path));
        builder.setCRS(featureType.getCoordinateReferenceSystem());

        featureType = builder.buildFeatureType();
        return featureType;
    }

    private Iterator<Feature> transformFeatures(SimpleFeatureType featureType, String path) {
        String refspec = Ref.WORK_HEAD + ":" + path;
        Iterator<NodeRef> oldFeatures = command(LsTreeOp.class).setReference(refspec)
                .setStrategy(Strategy.FEATURES_ONLY).call();

        RevFeatureType revFeatureType = RevFeatureType.build(featureType);
        Iterator<Feature> transformedIterator = transformIterator(oldFeatures, revFeatureType);
        return transformedIterator;
    }

    private Integer collectionSize(FeatureSource featureSource) {
        final Integer collectionSize;
        {
            int fastCount;
            try {
                fastCount = featureSource.getCount(Query.ALL);
            } catch (IOException e) {
                throw new GeoToolsOpException(e, StatusCode.UNABLE_TO_GET_FEATURES);
            }
            collectionSize = -1 == fastCount ? null : Integer.valueOf(fastCount);
        }
        return collectionSize;
    }

    private String[] checkPreconditions() {
        if (dataStore == null) {
            throw new GeoToolsOpException(StatusCode.DATASTORE_NOT_DEFINED);
        }

        if ((table == null || table.isEmpty()) && !(all)) {
            throw new GeoToolsOpException(StatusCode.TABLE_NOT_DEFINED);
        }

        if (table != null && !table.isEmpty() && all) {
            throw new GeoToolsOpException(StatusCode.ALL_AND_TABLE_DEFINED);
        }
        String[] typeNames;
        if (all) {
            try {
                typeNames = dataStore.getTypeNames();
            } catch (Exception e) {
                throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_NAMES);
            }
            if (typeNames.length == 0) {
                throw new GeoToolsOpException(StatusCode.NO_FEATURES_FOUND);
            }
        } else {
            SimpleFeatureType schema;
            try {
                schema = dataStore.getSchema(table);
            } catch (IOException e) {
                throw new GeoToolsOpException(StatusCode.TABLE_NOT_FOUND);
            }
            Preconditions.checkNotNull(schema);
            typeNames = new String[] { table };
        }

        if (typeNames.length > 1 && alter && all) {
            throw new GeoToolsOpException(StatusCode.ALTER_AND_ALL_DEFINED);
        }
        return typeNames;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FeatureSource getFeatureSource(String typeName) {

        FeatureSource featureSource;
        try {
            featureSource = dataStore.getFeatureSource(typeName);
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_FEATURES);
        }

        return new ForwardingFeatureSource(featureSource) {

            @Override
            public FeatureCollection getFeatures(Query query) throws IOException {

                final FeatureCollection features = super.getFeatures(query);
                return new ForwardingFeatureCollection(features) {

                    @Override
                    public FeatureIterator features() {

                        final FeatureType featureType = getSchema();
                        final String fidPrefix = featureType.getName().getLocalPart() + ".";

                        FeatureIterator iterator = delegate.features();

                        return new FidAndFtReplacerIterator(iterator, fidAttribute, fidPrefix,
                                (SimpleFeatureType) featureType);
                    }
                };
            }
        };
    }

    /**
     * Replaces the default geotools fid with the string representation of the value of an
     * attribute.
     * 
     * If the specified attribute is null, does not exist or the value is null, an fid is created by
     * taking the default fid and removing the specified fidPrefix prefix from it.
     * 
     * It also replaces the feature type. This is used to avoid identical feature types (in terms of
     * attributes) coming from different data sources (such as to shapefiles with different names)
     * being considered different for having a different name. It is used in this importer class to
     * decorate the name of the feature type when importing into a given tree, using the name of the
     * tree.
     * 
     * The passed feature type should have the same attribute descriptions as the one to replace,
     * but no checking is performed to ensure that
     * 
     */
    private static class FidAndFtReplacerIterator extends ForwardingFeatureIterator<SimpleFeature> {

        private final String fidPrefix;

        private String attributeName;

        private SimpleFeatureType featureType;

        public FidAndFtReplacerIterator(FeatureIterator iterator, final String attributeName,
                String fidPrefix, SimpleFeatureType featureType) {
            super(iterator);
            this.attributeName = attributeName;
            this.fidPrefix = fidPrefix;
            this.featureType = featureType;

        }

        @Override
        public SimpleFeature next() {
            SimpleFeature next = super.next();
            if (attributeName == null) {
                String fid = next.getID();
                if (fid.startsWith(fidPrefix)) {
                    fid = fid.substring(fidPrefix.length());
                }
                return new FidAndFtOverrideFeature(next, fid, featureType);
            } else {
                Object value = next.getAttribute(attributeName);
                Preconditions.checkNotNull(value);
                return new FidAndFtOverrideFeature(next, value.toString(), featureType);
            }
        }
    }

    private void insert(final WorkingTree workTree, final String path,
            final FeatureSource featureSource, final ProgressListener taskProgress) {

        final Query query = new Query();
        CoordinateSequenceFactory coordSeq = new PackedCoordinateSequenceFactory();
        query.getHints().add(new Hints(Hints.JTS_COORDINATE_SEQUENCE_FACTORY, coordSeq));
        workTree.insert(path, featureSource, query, taskProgress);

    }

    private Iterator<Feature> transformIterator(Iterator<NodeRef> nodeIterator,
            final RevFeatureType newFeatureType) {

        Iterator<Feature> iterator = Iterators.transform(nodeIterator,
                new Function<NodeRef, Feature>() {
                    @Override
                    public Feature apply(NodeRef node) {
                        return alter(node, newFeatureType);
                    }

                });

        return iterator;

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
     * @param all if this is set, all tables from the data store will be imported
     * @return {@code this}
     */
    public ImportOp setAll(boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @param table if this is set, only the specified table will be imported from the data store
     * @return {@code this}
     */
    public ImportOp setTable(String table) {
        this.table = table;
        return this;
    }

    /**
     * 
     * @param overwrite If this is true, existing features will be overwritten in case they exist
     *        and have the same path and Id than the features to import. If this is false, existing
     *        features will not be overwritten, and a safe import is performed, where only those
     *        features that do not already exists are added
     * @return {@code this}
     */
    public ImportOp setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    /**
     * 
     * @param the attribute to use to create the feature id, if the default.
     */
    public ImportOp setFidAttribute(String attribute) {
        this.fidAttribute = attribute;
        return this;
    }

    /**
     * @param force if true, it will change the default feature type of the tree we are importing
     *        into and change all features under that tree to have that same feature type
     * @return {@code this}
     */
    public ImportOp setAlter(boolean alter) {
        this.alter = alter;
        return this;
    }

    /**
     * 
     * @param destPath the path to import to to. If not provided, it will be taken from the feature
     *        type of the table to import.
     * @return {@code this}
     */
    public ImportOp setDestinationPath(@Nullable String destPath) {
        Preconditions.checkArgument(destPath == null || !destPath.isEmpty());
        this.destPath = destPath;
        return this;
    }

    /**
     * @param dataStore the data store to use for the import process
     * @return {@code this}
     */
    public ImportOp setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return this;
    }

    private static final class FidAndFtOverrideFeature extends DecoratingFeature {

        private String fid;

        private SimpleFeatureType featureType;

        public FidAndFtOverrideFeature(SimpleFeature delegate, String fid,
                SimpleFeatureType featureType) {
            super(delegate);
            this.fid = fid;
            this.featureType = featureType;
        }

        @Override
        public SimpleFeatureType getType() {
            return featureType;
        }

        @Override
        public String getID() {
            return fid;
        }

        @Override
        public FeatureId getIdentifier() {
            return new FeatureIdImpl(fid);
        }
    }

    /**
     * Sets the name to use for the geometry descriptor. If not provided, the geometry name from the
     * source schema will be used.
     * 
     * @param geomName
     */
    public ImportOp setGeometryNameOverride(String geomName) {
        this.geomName = geomName;
        return this;

    }

    public ImportOp setUsePaging(boolean usePaging) {
        this.usePaging = usePaging;
        return this;
    }
}
>>>>>>> .merge_file_btVBJA
