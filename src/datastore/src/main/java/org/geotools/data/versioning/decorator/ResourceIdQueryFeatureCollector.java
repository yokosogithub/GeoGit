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

import java.util.Iterator;
import java.util.Set;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.StagingDatabase;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.identity.ResourceId;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public class ResourceIdQueryFeatureCollector implements Iterable<Feature> {

    private final GeoGIT geogit;

    private final FeatureType featureType;

    private final Set<ResourceId> resourceIds;

    private Query query;

    public ResourceIdQueryFeatureCollector(final GeoGIT geogit, final FeatureType featureType,
            final Set<ResourceId> resourceIds, Query query) {
        this.geogit = geogit;
        this.featureType = featureType;
        this.resourceIds = resourceIds;
        this.query = query;
    }

    @Override
    public Iterator<Feature> iterator() {

        Iterator<Ref> featureRefs = Iterators.emptyIterator();

        VersionQuery versionQuery = new VersionQuery(geogit, featureType.getName());
        try {
            for (ResourceId rid : resourceIds) {
                Iterator<Ref> ridIterator;
                ridIterator = versionQuery.get(rid);
                featureRefs = Iterators.concat(featureRefs, ridIterator);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        featureRefs = versionQuery.filterByQueryVersion(featureRefs, query);

        Iterator<Feature> features = Iterators.transform(featureRefs, new RefToFeature(geogit,
                featureType));

        return features;
    }

    private final class RefToFeature implements Function<Ref, Feature> {

        private final GeoGIT geogit;

        private final FeatureType type;

        public RefToFeature(final GeoGIT geogit, final FeatureType type) {
            this.geogit = geogit;
            this.type = type;
        }

        @Override
        public Feature apply(final Ref featureRef) {
            String featureId = featureRef.getName();
            ObjectId contentId = featureRef.getObjectId();
            StagingDatabase database = geogit.getRepository().getIndex().getDatabase();
            Feature feature;

            ObjectReader<Feature> featureReader = geogit.getRepository().newFeatureReader(type,
                    featureId);
            feature = database.get(contentId, featureReader);

            return VersionedFeatureWrapper.wrap(feature, featureRef.getObjectId().toString());
        }

    }

}
