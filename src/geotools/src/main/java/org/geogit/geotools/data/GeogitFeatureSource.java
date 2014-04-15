/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nullable;

import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.MaxFeatureReader;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.Transaction;
import org.geotools.data.sort.SortedFeatureReader;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 *
 */
@SuppressWarnings("unchecked")
class GeogitFeatureSource extends ContentFeatureSource {

    /**
     * <b>Precondition</b>: {@code entry.getDataStore() instanceof GeoGitDataStore}
     * 
     * @param entry
     */
    public GeogitFeatureSource(ContentEntry entry) {
        this(entry, (Query) null);
    }

    /**
     * <b>Precondition</b>: {@code entry.getDataStore() instanceof GeoGitDataStore}
     * 
     * @param entry
     * @param query optional "definition query" making this feature source a "view"
     */
    public GeogitFeatureSource(ContentEntry entry, @Nullable Query query) {
        super(entry, query);
        Preconditions.checkArgument(entry.getDataStore() instanceof GeoGitDataStore);
    }

    /**
     * Adds the {@link Hints#FEATURE_DETACHED} hint to the supported hints so the renderer doesn't
     * clone the geometries
     */
    @Override
    protected void addHints(Set<Hints.Key> hints) {
        hints.add(Hints.FEATURE_DETACHED);
    }

    @Override
    protected boolean canFilter() {
        return true;
    }

    @Override
    protected boolean canSort() {
        return true;
    }

    @Override
    protected boolean canRetype() {
        return false;
    }

    @Override
    protected boolean canLimit() {
        return true;
    }

    @Override
    protected boolean canOffset() {
        return true;
    }

    /**
     * @return {@code true}
     */
    @Override
    protected boolean canTransact() {
        return true;
    }

    @Override
    protected boolean handleVisitor(Query query, FeatureVisitor visitor) throws IOException {
        return false;
    }

    @Override
    public GeoGitDataStore getDataStore() {
        return (GeoGitDataStore) super.getDataStore();
    }

    @Override
    public ContentState getState() {
        return super.getState();
    }

    /**
     * Overrides {@link ContentFeatureSource#getName()} to restore back the original meaning of
     * {@link FeatureSource#getName()}
     */
    @Override
    public Name getName() {
        return getEntry().getName();
    }

    /**
     * Creates a {@link QueryCapabilities} that declares support for
     * {@link QueryCapabilities#isUseProvidedFIDSupported() isUseProvidedFIDSupported}, the
     * datastore supports using the provided feature id in the data insertion workflow as opposed to
     * generating a new id, by looking into the user data map ( {@link Feature#getUserData()}) for a
     * {@link Hints#USE_PROVIDED_FID} key associated to a {@link Boolean#TRUE} value, if the
     * key/value pair is there an attempt to use the provided id will be made, and the operation
     * will fail if the key cannot be parsed into a valid storage identifier.
     */
    @Override
    protected QueryCapabilities buildQueryCapabilities() {
        return new QueryCapabilities() {
            /**
             * @return {@code true}
             */
            @Override
            public boolean isUseProvidedFIDSupported() {
                return true;
            }

            /**
             * 
             * @return {@code false} by now, will see how/whether we'll support
             *         {@link Query#getVersion()} later
             */
            @Override
            public boolean isVersionSupported() {
                return false;
            }

        };
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        final Filter filter = (Filter) query.getFilter().accept(new SimplifyingFilterVisitor(),
                null);
        final CoordinateReferenceSystem crs = getSchema().getCoordinateReferenceSystem();
        if (Filter.INCLUDE.equals(filter)) {
            NodeRef typeRef = getTypeRef();
            ReferencedEnvelope bounds = new ReferencedEnvelope(crs);
            typeRef.getNode().expand(bounds);
            return bounds;
        }
        if (Filter.EXCLUDE.equals(filter)) {
            return ReferencedEnvelope.create(crs);
        }

        FeatureReader<SimpleFeatureType, SimpleFeature> features;
        if (isNaturalOrder(query.getSortBy())) {
            Integer offset = query.getStartIndex();
            Integer maxFeatures = query.getMaxFeatures() == Integer.MAX_VALUE ? null : query
                    .getMaxFeatures();
            features = getNativeReader(filter, offset, maxFeatures);
        } else {
            features = getReader(query);
        }
        ReferencedEnvelope bounds = new ReferencedEnvelope(crs);
        try {
            while (features.hasNext()) {
                bounds.expandToInclude((ReferencedEnvelope) features.next().getBounds());
            }
        } finally {
            features.close();
        }
        return bounds;
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        final Filter filter = (Filter) query.getFilter().accept(new SimplifyingFilterVisitor(),
                null);
        if (Filter.EXCLUDE.equals(filter)) {
            return 0;
        }

        final Integer offset = query.getStartIndex();
        final Integer maxFeatures = query.getMaxFeatures() == Integer.MAX_VALUE ? null : query
                .getMaxFeatures();

        int size;
        if (Filter.INCLUDE.equals(filter)) {
            RevTree tree = getTypeTree();
            size = (int) tree.size();
            if (offset != null) {
                size = size - offset.intValue();
            }
            if (maxFeatures != null) {
                size = Math.min(size, maxFeatures.intValue());
            }
            return size;
        }

        FeatureReader<SimpleFeatureType, SimpleFeature> features;
        if (isNaturalOrder(query.getSortBy())) {
            features = getNativeReader(filter, offset, maxFeatures);
        } else {
            features = getReader(query);
        }
        int count = 0;
        try {
            while (features.hasNext()) {
                features.next();
                count++;
            }
        } finally {
            features.close();
        }

        return count;
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(final Query query)
            throws IOException {

        FeatureReader<SimpleFeatureType, SimpleFeature> reader;

        final boolean naturalOrder = isNaturalOrder(query.getSortBy());
        final int startIndex = Optional.fromNullable(query.getStartIndex()).or(Integer.valueOf(0));
        final Integer maxFeatures = query.getMaxFeatures() == Integer.MAX_VALUE ? null : query
                .getMaxFeatures();
        final Filter filter = query.getFilter();

        if (naturalOrder) {
            reader = getNativeReader(filter, startIndex, maxFeatures);
        } else {
            reader = getNativeReader(filter, null, null);
            // sorting
            reader = new SortedFeatureReader(DataUtilities.simple(reader), query);
            if (startIndex > 0) {
                // skip the first n records
                for (int i = 0; i < startIndex && reader.hasNext(); i++) {
                    reader.next();
                }
            }
            if (maxFeatures != null && maxFeatures > 0) {
                reader = new MaxFeatureReader<SimpleFeatureType, SimpleFeature>(reader, maxFeatures);
            }
        }

        return reader;
    }

    private boolean isNaturalOrder(@Nullable SortBy[] sortBy) {
        if (sortBy == null || sortBy.length == 0
                || (sortBy.length == 1 && SortBy.NATURAL_ORDER.equals(sortBy[0]))) {
            return true;
        }
        return false;
    }

    private GeogitFeatureReader<SimpleFeatureType, SimpleFeature> getNativeReader(Filter filter,
            @Nullable Integer offset, @Nullable Integer maxFeatures) {

        filter = (Filter) filter.accept(new SimplifyingFilterVisitor(), null);

        GeogitFeatureReader<SimpleFeatureType, SimpleFeature> nativeReader;

        final String rootRef = getRootRef();
        final String featureTypeTreePath = getTypeTreePath();

        final SimpleFeatureType schema = getSchema();

        final CommandLocator commandLocator = getCommandLocator();

        nativeReader = new GeogitFeatureReader<SimpleFeatureType, SimpleFeature>(commandLocator,
                schema, filter, featureTypeTreePath, rootRef, offset, maxFeatures);

        return nativeReader;
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {

        SimpleFeatureType featureType = getNativeType();

        final Name name = featureType.getName();
        final Name assignedName = getEntry().getName();

        if (assignedName.getNamespaceURI() != null && !assignedName.equals(name)) {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.init(featureType);
            builder.setName(assignedName);
            featureType = builder.buildFeatureType();
        }
        return featureType;
    }

    CommandLocator getCommandLocator() {
        CommandLocator commandLocator = getDataStore().getCommandLocator(getTransaction());
        return commandLocator;
    }

    SimpleFeatureType getNativeType() {

        final NodeRef typeRef = getTypeRef();
        final String treePath = typeRef.path();
        final ObjectId metadataId = typeRef.getMetadataId();

        CommandLocator commandLocator = getCommandLocator();
        Optional<RevFeatureType> revType = commandLocator.command(RevObjectParse.class)
                .setObjectId(metadataId).call(RevFeatureType.class);

        if (!revType.isPresent()) {
            throw new IllegalStateException(String.format("Feature type for tree %s not found",
                    treePath));
        }

        SimpleFeatureType featureType = (SimpleFeatureType) revType.get().type();
        return featureType;
    }

    String getTypeTreePath() {
        NodeRef typeRef = getTypeRef();
        String path = typeRef.path();
        return path;
    }

    /**
     * @return
     */
    NodeRef getTypeRef() {
        GeoGitDataStore dataStore = getDataStore();
        Name name = getName();
        Transaction transaction = getTransaction();
        return dataStore.findTypeRef(name, transaction);
    }

    /**
     * @return
     */
    RevTree getTypeTree() {
        String refSpec = getRootRef() + ":" + getTypeTreePath();
        CommandLocator commandLocator = getCommandLocator();
        Optional<RevTree> ref = commandLocator.command(RevObjectParse.class).setRefSpec(refSpec)
                .call(RevTree.class);
        Preconditions.checkState(ref.isPresent(), "Ref %s not found on working tree", refSpec);
        return ref.get();
    }

    private String getRootRef() {
        GeoGitDataStore dataStore = getDataStore();
        Transaction transaction = getTransaction();
        return dataStore.getRootRef(transaction);
    }

    /**
     * @return
     */
    WorkingTree getWorkingTree() {
        CommandLocator commandLocator = getCommandLocator();
        WorkingTree workingTree = commandLocator.getWorkingTree();
        return workingTree;
    }
}
