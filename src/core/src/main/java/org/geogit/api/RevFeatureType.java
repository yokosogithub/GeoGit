/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import javax.xml.namespace.QName;

/**
 * A binary representation of the state of a Feature Type.
 */
public abstract class RevFeatureType extends AbstractRevObject {

    private final Object parsedType;

    public RevFeatureType(Object parsed) {
        this(ObjectId.NULL, parsed);
    }

    public RevFeatureType(ObjectId id, Object parsed) {
        super(id, TYPE.FEATURETYPE);
        this.parsedType = parsed;
    }

    public Object type() {
        return parsedType;
    }

    /**
     * @return
     */
    public abstract QName getName();
}
