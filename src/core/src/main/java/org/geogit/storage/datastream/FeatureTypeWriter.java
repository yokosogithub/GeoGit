/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import static org.geogit.storage.datastream.FormatCommon.writeHeader;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.geogit.api.RevFeatureType;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.datastream.FormatCommon.FieldType;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class FeatureTypeWriter implements ObjectWriter<RevFeatureType> {
    @Override
    public void write(RevFeatureType object, OutputStream out) throws IOException {
        DataOutput data = new DataOutputStream(out);
        writeHeader(data, "featuretype");
        writeName(object.getName(), data);
        data.writeInt(object.sortedDescriptors().size());
        for (PropertyDescriptor desc : object.type().getDescriptors()) {
            writeProperty(desc, data);
        }
    }

    private void writeName(Name name, DataOutput data) throws IOException {
        final String ns = name.getNamespaceURI();
        final String lp = name.getLocalPart();
        data.writeUTF(ns == null ? "" : ns);
        data.writeUTF(lp == null ? "" : lp);
    }

    private void writePropertyType(PropertyType type, DataOutput data) throws IOException {
        writeName(type.getName(), data);
        data.writeByte(FieldType.forBinding(type.getBinding()).getTag());
        if (type instanceof GeometryType) {
            GeometryType gType = (GeometryType) type;
            CoordinateReferenceSystem crs = gType.getCoordinateReferenceSystem();
            if (crs == null) {
                data.writeBoolean(true);
                data.writeUTF("EPSG:0");
            } else {
                Integer code;
                try {
                    code = CRS.lookupEpsgCode(crs, true);
                } catch (FactoryException e) {
                    // TODO: Log this exception?
                    code = null;
                }

                if (code != null) {
                    data.writeBoolean(true);
                    data.writeUTF("EPSG:" + code);
                } else {
                    data.writeBoolean(false);
                    data.writeUTF(crs.toWKT());
                }
            }
        }
    }

    private void writeProperty(PropertyDescriptor attr, DataOutput data) throws IOException {
        writeName(attr.getName(), data);
        data.writeBoolean(attr.isNillable());
        data.writeInt(attr.getMinOccurs());
        data.writeInt(attr.getMaxOccurs());
        writePropertyType(attr.getType(), data);
    }
}
