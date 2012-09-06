/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.versioning.decorator;

import java.io.IOException;
import java.util.Iterator;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.StagingDatabase;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public class QueryFeatureCollector implements Iterable<Feature> {

    private final GeoGIT geogit;

    private final FeatureType featureType;

    private Query query;

    public QueryFeatureCollector(final GeoGIT repository, final FeatureType featureType, Query query) {
        this.geogit = repository;
        this.featureType = featureType;
        this.query = query;
    }

    @Override
    public Iterator<Feature> iterator() {

        VersionQuery versionQuery = new VersionQuery(geogit, featureType.getName());
        Iterator<Ref> featureRefs;
        try {
            featureRefs = versionQuery.getByQuery(query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Iterator<Feature> features = Iterators.transform(featureRefs, new RefToFeature(geogit,
                featureType));

        return features;
    }

    private final class RefToFeature implements Function<Ref, Feature> {

        private final GeoGIT geogit;

        private final FeatureType type;

        public RefToFeature(final GeoGIT repo, final FeatureType type) {
            this.geogit = repo;
            this.type = type;
        }

        @Override
        public Feature apply(final Ref featureRef) {
            String featureId = featureRef.getName();
            ObjectId contentId = featureRef.getObjectId();
            StagingDatabase database = geogit.getRepository().getIndex().getDatabase();
            Feature feature;
            try {
                ObjectReader<Feature> featureReader = geogit.getRepository().newFeatureReader(type,
                        featureId);
                feature = database.get(contentId, featureReader);
                if (!feature.getType().equals(type)) {
                    throw new IOException("Invalid feature type returned.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return VersionedFeatureWrapper.wrap(feature, featureRef.getObjectId().toString());
        }

    }

}
