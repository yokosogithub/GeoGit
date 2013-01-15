/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geogit.api.FeatureBuilder;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geotools.data.FeatureReader;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 *
 */
public class GeogitFeatureReader<T extends FeatureType, F extends Feature> implements
        FeatureReader<T, F> {

    private GeoGIT geogit;

    private SimpleFeatureType schema;

    private FeatureBuilder featureBuilder;

    private Filter filter;

    private Iterator<NodeRef> featureRefs;

    /**
     * @param geogit
     * @param schema
     * @param typeTree
     * @param filter
     */
    public GeogitFeatureReader(GeoGIT geogit, SimpleFeatureType schema, NodeRef treeRef,
            Filter filter) {
        this.geogit = geogit;
        this.schema = schema;
        this.filter = filter;
        String refSpec = Ref.WORK_HEAD + ":" + treeRef.path();
        this.featureRefs = geogit.command(LsTreeOp.class).setStrategy(Strategy.FEATURES_ONLY)
                .setReference(refSpec).call();
        this.featureBuilder = new FeatureBuilder(schema);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getFeatureType() {
        return (T) schema;
    }

    @Override
    public void close() throws IOException {
        //
    }

    @Override
    public boolean hasNext() throws IOException {
        return featureRefs.hasNext();
    }

    @SuppressWarnings("unchecked")
    @Override
    public F next() throws IOException, IllegalArgumentException, NoSuchElementException {
        NodeRef featureRef = featureRefs.next();
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setObjectId(featureRef.objectId()).call(RevFeature.class);
        Preconditions.checkState(revFeature.isPresent());

        String id = NodeRef.nodeFromPath(featureRef.path());
        Feature feature = featureBuilder.build(id, revFeature.get());
        return (F) feature;
    }

}
