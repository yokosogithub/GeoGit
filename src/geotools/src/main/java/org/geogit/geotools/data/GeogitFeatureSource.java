/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;

import javax.annotation.Nullable;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RevObjectParse;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

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
        FeatureReader<SimpleFeatureType, SimpleFeature> features = getReader(query);
        CoordinateReferenceSystem crs = features.getFeatureType().getCoordinateReferenceSystem();
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

        Filter filter = query.getFilter();

        FilterFactory2 factory = CommonFactoryFinder.getFilterFactory2();
        FeatureType featureType = getSchema();
        ReprojectingFilterVisitor reprojectingFilterVisitor = new ReprojectingFilterVisitor(
                factory, featureType);

        Filter nativeCrsFilter = (Filter) filter.accept(reprojectingFilterVisitor, null);

        GeogitFeatureReader<SimpleFeatureType, SimpleFeature> nativeReader = new GeogitFeatureReader<SimpleFeatureType, SimpleFeature>(
                getGeogit(), getSchema(), getTypeRef(), nativeCrsFilter);

        return new FilteringFeatureReader<SimpleFeatureType, SimpleFeature>(nativeReader,
                nativeCrsFilter);
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
