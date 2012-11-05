/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.repository;

import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class FeatureBuilder {

    private RevFeatureType type;

    private SimpleFeatureBuilder featureBuilder;

    public FeatureBuilder(RevFeatureType type) {
        this.type = type;
        featureBuilder = new SimpleFeatureBuilder((SimpleFeatureType) type.type());
    }

    public FeatureBuilder(SimpleFeatureType type) {
        this(new RevFeatureType(type));
    }

    public Feature build(String id, RevFeature revFeature) {
        featureBuilder.reset();
        ImmutableList<PropertyDescriptor> descriptors = type.sortedDescriptors();
        ImmutableList<Optional<Object>> values = revFeature.getValues();
        Preconditions.checkState(descriptors.size() == values.size());

        for (int i = 0; i < descriptors.size(); i++) {
            PropertyDescriptor descriptor = descriptors.get(i);
            Object value = values.get(i).orNull();
            featureBuilder.set(descriptor.getName(), value);
        }

        SimpleFeature feature = featureBuilder.buildFeature(id);
        return feature;
    }
}
