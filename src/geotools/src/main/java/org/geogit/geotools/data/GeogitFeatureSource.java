/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nullable;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RevObjectParse;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.filter.visitor.SpatialFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Envelope;

/**
 *
 */
@SuppressWarnings("unchecked")
public class GeogitFeatureSource extends ContentFeatureSource {

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
        return false;
    }

    @Override
    protected boolean canRetype() {
        return false;
    }

    @Override
    protected boolean canLimit() {
        return false;
    }

    @Override
    protected boolean canOffset() {
        return false;
    }

    @Override
    protected boolean canTransact() {
        return false;
    }

    @Override
    protected boolean handleVisitor(Query query, FeatureVisitor visitor) throws IOException {
        return false;
    }

    /**
     * shortcut for {@code getDataStore().getGeogit()}
     */
    GeoGIT getGeogit() {
        return getDataStore().getGeogit();
    }

    @Override
    public GeoGitDataStore getDataStore() {
        return (GeoGitDataStore) super.getDataStore();
    }

    @Override
    public GeogitTransactionState getState() {
        return (GeogitTransactionState) super.getState();
    }

    /**
     * Overrides {@link ContentFeatureSource#getName()} to restore back the original meaning of
     * {@link FeatureSource#getName()}
     */
    @Override
    public Name getName() {
        return getEntry().getName();
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        // TODO optimize, please
        final CoordinateReferenceSystem crs = getSchema().getCoordinateReferenceSystem();
        if (Filter.INCLUDE.equals(query.getFilter())) {
            NodeRef typeRef = getDataStore().findTypeRef(getName());
            ReferencedEnvelope bounds = new ReferencedEnvelope(crs);
            typeRef.getNode().expand(bounds);
            return bounds;
        }
        FeatureReader<SimpleFeatureType, SimpleFeature> features = getReader(query);
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
        final Filter filter = query.getFilter();
        if (Filter.EXCLUDE.equals(filter)) {
            return 0;
        }

        int size;
        if (Filter.INCLUDE.equals(filter)) {
            RevTree tree = getTypeTree();
            size = (int) tree.size();
            return size;
        }

        // TODO optimize, please
        FeatureReader<SimpleFeatureType, SimpleFeature> features = getReader(query);
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

    /**
     * @return
     */
    private RevTree getTypeTree() {
        NodeRef typeRef = getDataStore().findTypeRef(getName());
        return getGeogit().command(RevObjectParse.class).setObjectId(typeRef.objectId())
                .call(RevTree.class).get();
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {

        final Filter queryFilter = (Filter) query.getFilter().accept(
                new SimplifyingFilterVisitor(), null);
        Filter filter = queryFilter;
        GeogitFeatureReader<SimpleFeatureType, SimpleFeature> nativeReader;

        final GeoGIT geogit = getGeogit();
        final SimpleFeatureType schema = getSchema();
        final NodeRef typeRef = getTypeRef();

        Envelope queryBounds = null;

        if (hasSpatialFilter(queryFilter)) {
            FilterFactory2 factory = CommonFactoryFinder.getFilterFactory2();
            filter = (Filter) queryFilter.accept(new ReprojectingFilterVisitor(factory, schema),
                    null);
            queryBounds = (Envelope) filter.accept(new ExtractBounds(), queryBounds);
        }

        nativeReader = new GeogitFeatureReader<SimpleFeatureType, SimpleFeature>(geogit, schema,
                typeRef, filter, queryBounds);

        return nativeReader;
    }

    private boolean hasSpatialFilter(Filter filter) {
        SpatialFilterVisitor spatialFilterVisitor = new SpatialFilterVisitor();
        filter.accept(spatialFilterVisitor, null);
        return spatialFilterVisitor.hasSpatialFilter();
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {

        final NodeRef typeRef = getTypeRef();
        final String treePath = typeRef.path();
        final ObjectId metadataId = typeRef.getMetadataId();

        final GeoGIT geogit = getGeogit();

        Optional<RevFeatureType> revType = geogit.command(RevObjectParse.class)
                .setObjectId(metadataId).call(RevFeatureType.class);
        if (revType.isPresent()) {
            SimpleFeatureType featureType = (SimpleFeatureType) revType.get().type();
            return featureType;
        }

        throw new IllegalStateException(String.format("Feature type for tree %s not found",
                treePath));
    }

    /**
     * @return
     */
    private NodeRef getTypeRef() {
        return getDataStore().findTypeRef(getName());
    }
}
