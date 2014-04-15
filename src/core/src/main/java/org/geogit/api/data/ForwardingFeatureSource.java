/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.data;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.Set;

import org.geotools.data.DataAccess;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public class ForwardingFeatureSource<T extends FeatureType, F extends Feature> implements
        FeatureSource<T, F> {

    private FeatureSource<T, F> source;

    public ForwardingFeatureSource(FeatureSource<T, F> source) {
        this.source = source;
    }

    @Override
    public Name getName() {
        return source.getName();
    }

    @Override
    public ResourceInfo getInfo() {
        return source.getInfo();
    }

    @Override
    public DataAccess<T, F> getDataStore() {
        return source.getDataStore();
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return source.getQueryCapabilities();
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
        source.addFeatureListener(listener);
    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
        source.removeFeatureListener(listener);
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Filter filter) throws IOException {
        return source.getFeatures(filter);
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        return source.getFeatures(query);
    }

    @Override
    public FeatureCollection<T, F> getFeatures() throws IOException {
        return source.getFeatures();
    }

    @Override
    public T getSchema() {
        return source.getSchema();
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return source.getBounds();
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return source.getBounds(query);
    }

    @Override
    public int getCount(Query query) throws IOException {
        return source.getCount(query);
    }

    @Override
    public Set<Key> getSupportedHints() {
        return source.getSupportedHints();
    }

}
