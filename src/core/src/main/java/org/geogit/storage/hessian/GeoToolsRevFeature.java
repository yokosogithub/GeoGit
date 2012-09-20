/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage.hessian;

import javax.xml.namespace.QName;

import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geotools.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;

/**
 *
 */
public class GeoToolsRevFeature extends RevFeature {

    public GeoToolsRevFeature(Feature feature) {
        super(feature);
    }

    public GeoToolsRevFeature(ObjectId objectId, Feature feature) {
        super(objectId, feature);
    }

    @Override
    public RevFeatureType getFeatureType() {
        return new GeoToolsRevFeatureType(feature().getType());
    }

    @Override
    public Feature feature() {
        return (Feature) super.feature();
    }

    @Override
    public BoundingBox getBounds() {
        return feature().getBounds();
    }

    @Override
    public QName getName() {
        Name name = feature().getName();
        String namespaceURI = name.getNamespaceURI();
        String localPart = name.getLocalPart();
        return new QName(namespaceURI, localPart);
    }

    @Override
    public String getFeatureId() {
        return feature().getIdentifier().getID();
    }

    @Override
    public boolean isUseProvidedFid() {
        Object hint = feature().getUserData().get(Hints.USE_PROVIDED_FID);
        return Boolean.TRUE.equals(hint);
    }
}
