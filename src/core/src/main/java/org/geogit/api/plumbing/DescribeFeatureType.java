/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;

/**
 * Retrieves the set of property descriptors for the given feature type.
 */
public class DescribeFeatureType extends AbstractGeoGitOp<ImmutableSortedSet<PropertyDescriptor>> {

    private RevFeatureType featureType;

    private static final Ordering<PropertyDescriptor> PROPERTY_DESCRIPTOR_ORDER = new Ordering<PropertyDescriptor>() {
        @Override
        public int compare(PropertyDescriptor left, PropertyDescriptor right) {
            int c = Ordering.natural().nullsFirst()
                    .compare(left.getName().getNamespaceURI(), right.getName().getNamespaceURI());
            if (c == 0) {
                c = Ordering.natural().nullsFirst()
                        .compare(left.getName().getLocalPart(), right.getName().getLocalPart());
            }
            return c;
        }
    };

    /**
     * @param feature type to describe
     */
    public DescribeFeatureType setFeatureType(RevFeatureType featureType) {
        this.featureType = featureType;
        return this;
    }

    /**
     * Retrieves the set of property descriptors for the given feature type.
     * 
     * @return a sorted set of all the property descriptors of the feature type.
     */
    @Override
    public ImmutableSortedSet<PropertyDescriptor> call() {
        Preconditions.checkState(featureType != null, "FeatureType has not been set.");

        FeatureType type = featureType.type();

        ImmutableSortedSet.Builder<PropertyDescriptor> propertySetBuilder = new ImmutableSortedSet.Builder<PropertyDescriptor>(
                PROPERTY_DESCRIPTOR_ORDER);

        propertySetBuilder.addAll(type.getDescriptors());

        return propertySetBuilder.build();
    }
}
