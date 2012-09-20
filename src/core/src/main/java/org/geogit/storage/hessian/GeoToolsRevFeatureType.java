/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage.hessian;

import javax.xml.namespace.QName;

import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 *
 */
public class GeoToolsRevFeatureType extends RevFeatureType {

    public GeoToolsRevFeatureType(FeatureType parsed) {
        super(parsed);
    }

    public GeoToolsRevFeatureType(ObjectId id, FeatureType parsed) {
        super(id, parsed);
    }

    @Override
    public FeatureType type() {
        return (FeatureType) super.type();
    }

    @Override
    public QName getName() {
        Name name = type().getName();
        return new QName(name.getNamespaceURI(), name.getLocalPart());
    }

}
