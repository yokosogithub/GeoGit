/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;
import java.util.Collections;

import javax.xml.namespace.QName;

import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * A binary representation of the state of a Feature Type.
 */
public class RevFeatureType extends AbstractRevObject {

    private final FeatureType featureType;

    private ImmutableList<PropertyDescriptor> sortedDescriptors;

    public static final Ordering<PropertyDescriptor> PROPERTY_ORDER = new Ordering<PropertyDescriptor>() {
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

    public RevFeatureType(FeatureType featureType) {
        this(ObjectId.NULL, featureType);
    }

    public RevFeatureType(ObjectId id, FeatureType featureType) {
        super(id, TYPE.FEATURETYPE);
        this.featureType = featureType;
        ArrayList<PropertyDescriptor> descriptors = Lists.newArrayList(this.featureType
                .getDescriptors());
        Collections.sort(descriptors, PROPERTY_ORDER);
        sortedDescriptors = ImmutableList.copyOf(descriptors);

    }

    public FeatureType type() {
        return featureType;
    }

    public ImmutableList<PropertyDescriptor> sortedDescriptors() {
        return sortedDescriptors;
    }

    /**
     * @return
     */
    public QName getName() {
        Name name = type().getName();
        return new QName(name.getNamespaceURI(), name.getLocalPart());
    }
}
