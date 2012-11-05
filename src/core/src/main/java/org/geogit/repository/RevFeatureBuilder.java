/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

public final class RevFeatureBuilder {

    private static final Ordering<Property> PROPERTY_ORDER = new Ordering<Property>() {
        @Override
        public int compare(Property left, Property right) {
            return RevFeatureType.PROPERTY_ORDER.compare(left.getDescriptor(),
                    right.getDescriptor());
        }
    };

    public RevFeature build(Feature feature) {
        if (feature == null) {
            throw new IllegalStateException("No feature set");
        }

        Collection<Property> props = feature.getProperties();

        List<Property> list = new ArrayList<Property>(props);

        Collections.sort(list, PROPERTY_ORDER);

        ImmutableList.Builder<Optional<Object>> valuesBuilder = new ImmutableList.Builder<Optional<Object>>();

        for (Property prop : list) {
            valuesBuilder.add(Optional.fromNullable(prop.getValue()));
        }

        return new RevFeature(valuesBuilder.build());
    }
}
