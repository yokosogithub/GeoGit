/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import javax.xml.namespace.QName;

import org.opengis.geometry.BoundingBox;

/**
 * A binary representation of the state of a Feature.
 * 
 */
public abstract class RevFeature extends AbstractRevObject {

    private final Object parsed;

    public RevFeature(Object feature) {
        this(ObjectId.NULL, feature);
    }

    public RevFeature(ObjectId id, Object parsed) {
        super(id, TYPE.FEATURE);
        this.parsed = parsed;
    }

    public abstract RevFeatureType getFeatureType();

    public Object feature() {
        return parsed;
    }

    /**
     * @return
     */
    public abstract BoundingBox getBounds();

    /**
     * @return
     */
    public abstract QName getName();

    /**
     * @return
     */
    public abstract String getFeatureId();

    /**
     * @return
     */
    public abstract boolean isUseProvidedFid();
}
