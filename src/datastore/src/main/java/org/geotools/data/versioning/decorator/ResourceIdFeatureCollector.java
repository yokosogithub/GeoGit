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
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.StagingDatabase;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.identity.ResourceId;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public class ResourceIdFeatureCollector implements Iterable<Feature> {

    private final GeoGIT geogit;

    private final FeatureType featureType;

    private final Set<ResourceId> resourceIds;

    public ResourceIdFeatureCollector(final GeoGIT repository, final FeatureType featureType,
            final Set<ResourceId> resourceIds) {
        this.geogit = repository;
        this.featureType = featureType;
        this.resourceIds = resourceIds;
    }

    @Override
    public Iterator<Feature> iterator() {

        Iterator<NodeRef> featureRefs = Iterators.emptyIterator();

        VersionQuery query = new VersionQuery(geogit, featureType.getName());
        try {
            for (ResourceId rid : resourceIds) {
                Iterator<NodeRef> ridIterator;
                ridIterator = query.get(rid);
                featureRefs = Iterators.concat(featureRefs, ridIterator);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Iterator<Feature> features = Iterators.transform(featureRefs, new RefToFeature(geogit,
                featureType));

        return features;
    }

    private final class RefToFeature implements Function<NodeRef, Feature> {

        private final GeoGIT repo;

        private final FeatureType type;

        public RefToFeature(final GeoGIT repo, final FeatureType type) {
            this.repo = repo;
            this.type = type;
        }

        @Override
        public Feature apply(final NodeRef featureRef) {
            String featureId = featureRef.getName();
            ObjectId contentId = featureRef.getObjectId();
            StagingDatabase database = repo.getRepository().getIndex().getDatabase();
            Feature feature;

            ObjectReader<Feature> featureReader = geogit.getRepository().newFeatureReader(type,
                    featureId);
            feature = database.get(contentId, featureReader);

            return VersionedFeatureWrapper.wrap(feature, featureRef.getObjectId().toString());
        }

    }

}
