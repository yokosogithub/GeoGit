/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api;

import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.filter.identity.FeatureIdVersionedImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.identity.FeatureId;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Provides a method of building features from {@link RevFeature} objects that have the type
 * specified by the given {@link RevFeatureType}.
 * 
 * @see RevFeatureType
 * @see RevFeature
 * @see Feature
 */
public class FeatureBuilder {

    private RevFeatureType type;

    private FeatureType featureType;

    /**
     * Constructs a new {@code FeatureBuilder} with the given {@link RevFeatureType feature type}.
     * 
     * @param type the feature type of the features that will be built
     */
    public FeatureBuilder(RevFeatureType type) {
        this.type = type;
        this.featureType = type.type();
    }

    /**
     * Constructs a new {@code FeatureBuilder} with the given {@link SimpleFeatureType feature type}
     * .
     * 
     * @param type the feature type of the features that will be built
     */
    public FeatureBuilder(SimpleFeatureType type) {
        this(RevFeatureType.build(type));
    }

    /**
     * Builds a {@link Feature} from the provided {@link RevFeature}.
     * 
     * @param id the id of the new feature
     * @param revFeature the {@code RevFeature} with the property values for the feature
     * @return the constructed {@code Feature}
     */
    public Feature build(final String id, final RevFeature revFeature) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(revFeature);

        final String version = revFeature.getId().toString();
        final FeatureId fid = new FeatureIdVersionedImpl(id, version);
        final int attCount = featureType.getDescriptors().size();
        Object[] rawValues = new Object[attCount];

        SimpleFeature feature = new SimpleFeatureImpl(rawValues, (SimpleFeatureType) featureType,
                fid, false);

        ImmutableList<PropertyDescriptor> descriptors = type.sortedDescriptors();
        ImmutableList<Optional<Object>> values = revFeature.getValues();
        Preconditions.checkState(descriptors.size() == values.size());

        for (int i = 0; i < descriptors.size(); i++) {
            PropertyDescriptor descriptor = descriptors.get(i);
            Object value = values.get(i).orNull();
            feature.setAttribute(descriptor.getName(), value);
        }

        return feature;
    }
}
