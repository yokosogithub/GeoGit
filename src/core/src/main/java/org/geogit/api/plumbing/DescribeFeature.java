/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevFeature;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;

import com.google.common.collect.ImmutableSortedSet;

/**
 * Describes all of the properties in the given {@link RevFeature}.
 * 
 * @author jgarrett
 * @see RevFeature
 */
public class DescribeFeature extends AbstractGeoGitOp<ImmutableSortedSet<Property>> {

    private RevFeature feature;

    private FeatureType featureType;

    /**
     * @param feature the feature to describe
     * @return {@code this}
     */
    public DescribeFeature setFeature(RevFeature feature) {
        this.feature = feature;
        return this;
    }

    /**
     * @param featureType the {@link FeatureType} associated with this feature
     * @return {@code this}
     */
    public DescribeFeature setFeatureType(FeatureType featureType) {
        this.featureType = featureType;
        return this;
    }

    /**
     * Retrieves all properites from the given feature.
     * 
     * @return a sorted set of all the properites.
     */
    @Override
    public ImmutableSortedSet<Property> call() {
        // Preconditions.checkState(feature != null, "Feature has not been set.");

        // Collection<PropertyDescriptor> props = featureType.getDescriptors();

        // List<PropertyDescriptor> list = new ArrayList<PropertyDescriptor>(props);

        // Collections.sort(list, PROPERTY_DESCRIPTOR_ORDER);

        // ImmutableSortedSet.Builder<Property> propertySetBuilder = new
        // ImmutableSortedSet.Builder<Property>(
        // PROPERTY_ORDER);

        // Build Properties and add them to the propertySetBuilder.
        // ImmutableList<Optional<Object>> values = feature.getValues();

        // return propertySetBuilder.build();

        throw new UnsupportedOperationException();
    }
}
