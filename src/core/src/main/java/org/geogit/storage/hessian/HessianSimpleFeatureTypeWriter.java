/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.storage.GtEntityType;
import org.geogit.storage.ObjectWriter;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.wkt.Formattable;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.caucho.hessian.io.Hessian2Output;

/**
 * Writes a {@link SimpleFeatureType feature type} to a binary encoded stream.
 */
public class HessianSimpleFeatureTypeWriter implements ObjectWriter<RevFeatureType> {

    /**
     * Writes the provided feature type to the output stream.
     */
    @Override
    public void write(final RevFeatureType revType, OutputStream out) throws IOException {

        final SimpleFeatureType type = (SimpleFeatureType) revType.type();

        Hessian2Output hout = new Hessian2Output(out);
        try {
            hout.startMessage();
            hout.writeInt(RevObject.TYPE.FEATURETYPE.value());
            Name typeName = type.getName();
            hout.writeString(typeName.getNamespaceURI() == null ? "" : typeName.getNamespaceURI());
            hout.writeString(typeName.getLocalPart());
            List<AttributeDescriptor> descriptors = type.getAttributeDescriptors();
            hout.writeInt(descriptors.size());
            for (AttributeDescriptor descriptor : descriptors) {
                writeDescriptor(hout, descriptor);
            }

            hout.completeMessage();
        } finally {
            hout.flush();
            hout.close();
        }
    }

    /**
     * The format will be written as follows:
     * <ol>
     * <li>EntityType - int</li>
     * <li>nillable - boolean</li>
     * <li>property namespace - String</li>
     * <li>property name - String</li>
     * <li>max - int</li>
     * <li>min - int</li>
     * <li>type namespace - String</li>
     * <li>type name - String</li>
     * </ol>
     * If the entity type is a geometry, then there are additional fields,
     * <ol>
     * <li>geometry type - String</li>
     * <li>crs code - boolean</li>
     * <li>crs text - String</li>
     * </ol>
     * 
     * @param hout
     */
    private void writeDescriptor(Hessian2Output hout, AttributeDescriptor descriptor)
            throws IOException {
        AttributeType attrType = descriptor.getType();
        GtEntityType type = GtEntityType.fromBinding(attrType.getBinding());
        hout.writeInt(type.getValue());
        hout.writeBoolean(descriptor.isNillable());
        Name propertyName = descriptor.getName();
        hout.writeString(propertyName.getNamespaceURI() == null ? "" : propertyName
                .getNamespaceURI());
        hout.writeString(propertyName.getLocalPart());
        hout.writeInt(descriptor.getMaxOccurs());
        hout.writeInt(descriptor.getMinOccurs());
        Name typeName = attrType.getName();
        hout.writeString(typeName.getNamespaceURI() == null ? "" : typeName.getNamespaceURI());
        hout.writeString(typeName.getLocalPart());
        if (type.isGeometry() && attrType instanceof GeometryType) {
            GeometryType gt = (GeometryType) attrType;
            hout.writeObject(gt.getBinding());
            CoordinateReferenceSystem crs = gt.getCoordinateReferenceSystem();
            String srsName;
            if (crs == null) {
                srsName = "urn:ogc:def:crs:EPSG::0";
            } else {
                // use a flag to control whether the code is returned in EPSG: form instead of
                // urn:ogc:.. form irrespective of the org.geotools.referencing.forceXY System
                // property.
                final boolean longitudeFisrt = CRS.getAxisOrder(crs, false) == AxisOrder.EAST_NORTH;
                boolean codeOnly = true;
                String crsCode = CRS.toSRS(crs, codeOnly);
                if (crsCode != null) {
                    srsName = (longitudeFisrt ? "EPSG:" : "urn:ogc:def:crs:EPSG::") + crsCode;
                } else {
                    srsName = null;
                }
            }
            if (srsName != null) {
                hout.writeBoolean(true);
                hout.writeString(srsName);
            } else {
                String wkt;
                if (crs instanceof Formattable) {
                    wkt = ((Formattable) crs).toWKT(Formattable.SINGLE_LINE);
                } else {
                    wkt = crs.toWKT();
                }
                hout.writeBoolean(false);
                hout.writeString(wkt);
            }
        }
    }
}
