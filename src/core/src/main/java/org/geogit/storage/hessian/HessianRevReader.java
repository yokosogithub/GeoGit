/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.SpatialNode;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Throwables;

/**
 * Abstract parent class to readers of Rev's. This class provides some common functions used by
 * various Rev readers and printers.
 * 
 */
abstract class HessianRevReader {
    /**
     * Different types of tree nodes.
     */
    public enum Node {
        REF(0), BUCKET(1), END(2);

        private int value;

        /**
         * Constructs a new node enumeration with the given value.
         * 
         * @param value the value for the node
         */
        Node(int value) {
            this.value = value;
        }

        /**
         * @return the {@code int} value of this enumeration
         */
        public int getValue() {
            return this.value;
        }

        /**
         * Determines the {@code Node} given its integer value.
         * 
         * @param value The value of the desired {@code Node}
         * @return The correct {@code Node} for the value, or null if none is found.
         */
        public static Node fromValue(int value) {
            for (Node n : Node.values()) {
                if (value == n.getValue())
                    return n;
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, CoordinateReferenceSystem> crsCache = Collections
            .synchronizedMap(new LRUMap(3));

    /**
     * Constructs a new {@code HessianRevReader}.
     */
    public HessianRevReader() {
        super();
    }

    /**
     * Reads the ObjectId content from the given input stream and creates a new ObjectId object from
     * it.
     * 
     * @param hin the input stream
     * @return the new {@link ObjectId} or {@code ObjectId.NULL} if no bytes were read
     * @throws IOException
     */
    protected ObjectId readObjectId(Hessian2Input hin) throws IOException {
        byte[] bytes = hin.readBytes();
        if (bytes == null) {
            return ObjectId.NULL;
        }
        ObjectId id = new ObjectId(bytes);
        return id;
    }

    protected Ref readRef(Hessian2Input hin) throws IOException {
        TYPE type = TYPE.valueOf(hin.readInt());
        String name = hin.readString();
        ObjectId id = readObjectId(hin);

        Ref ref = new Ref(name, id, type);
        return ref;
    }

    protected org.geogit.api.Node readNode(Hessian2Input hin) throws IOException {
        TYPE type = TYPE.valueOf(hin.readInt());
        String name = hin.readString();
        ObjectId id = readObjectId(hin);
        ObjectId metadataId = readObjectId(hin);
        BoundingBox bbox = readBBox(hin);

        org.geogit.api.Node ref;
        if (bbox == null) {
            ref = new org.geogit.api.Node(name, id, metadataId, type);
        } else {
            ref = new SpatialNode(name, id, metadataId, type, bbox);
        }

        return ref;
    }

    /**
     * Reads the corner coordinates of a bounding box from the input stream.
     * 
     * A complete bounding box is encoded as four double values. An empty bounding box is encoded as
     * a single NaN value. In this case null is returned.
     * 
     * @param hin
     * @return The BoundingBox described in the stream, or null if none found.
     * @throws IOException
     */
    protected BoundingBox readBBox(Hessian2Input hin) throws IOException {
        double minx = hin.readDouble();
        if (Double.isNaN(minx))
            return null;

        double maxx = hin.readDouble();
        double miny = hin.readDouble();
        double maxy = hin.readDouble();

        String epsgCode = hin.readString();
        CoordinateReferenceSystem crs = null;
        if (epsgCode != null && epsgCode.length() > 0)
            crs = lookupCrs(epsgCode);

        BoundingBox bbox = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        return bbox;
    }

    private static CoordinateReferenceSystem lookupCrs(final String epsgCode) {
        CoordinateReferenceSystem crs = crsCache.get(epsgCode);
        if (crs == null) {
            try {
                crs = CRS.decode(epsgCode, false);
                crsCache.put(epsgCode, crs);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        return crs;
    }
}
