/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;

import com.caucho.hessian.io.Hessian2Output;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Abstract parent class to writers of Rev's. This class provides some common functions used by
 * various Rev writers.
 * 
 */
class HessianRevWriter {

    /**
     * Constructs a new {@code HessianRevWriter}.
     */
    public HessianRevWriter() {
        super();
    }

    /**
     * Writes out an ObjectId as an array of bytes.
     * 
     * @param hout
     * @param id
     * @throws IOException
     */
    protected void writeObjectId(Hessian2Output hout, ObjectId id) throws IOException {
        Preconditions.checkNotNull(id);
        if (id.isNull()) {
            hout.writeNull();
        } else {
            hout.writeBytes(id.getRawValue());
        }
    }

    protected void writeRef(Hessian2Output hout, Ref ref) throws IOException {
        hout.writeInt(HessianRevReader.Node.REF.getValue());
        hout.writeInt(ref.getType().value());
        hout.writeString(ref.getName());
        writeObjectId(hout, ref.getObjectId());
    }

    protected void writeNode(Hessian2Output hout, Node node, Envelope envHelper) throws IOException {
        hout.writeInt(HessianRevReader.Node.REF.getValue());
        hout.writeInt(node.getType().value());
        hout.writeString(node.getName());
        writeObjectId(hout, node.getObjectId());
        writeObjectId(hout, node.getMetadataId().or(ObjectId.NULL));
        writeBBox(hout, node, envHelper);
    }

    /**
     * Writes a BoundingBox to the provided output stream. The bounding box is encoded as four
     * double values, in the following order: - minx or westing - maxx or easting - miny or southing
     * - maxy or northing
     * 
     * A null bounding box is written as a single double NaN value.
     * 
     * @throws IOException
     */
    private void writeBBox(Hessian2Output hout, Node node, Envelope envHelper) throws IOException {
        envHelper.setToNull();
        node.expand(envHelper);
        if (envHelper.isNull()) {
            hout.writeDouble(Double.NaN);
            return;
        }

        hout.writeDouble(envHelper.getMinX());
        hout.writeDouble(envHelper.getMaxX());
        hout.writeDouble(envHelper.getMinY());
        hout.writeDouble(envHelper.getMaxY());

    }
}