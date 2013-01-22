/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Abstract parent class to readers of Rev's. This class provides some common functions used by
 * various Rev readers and printers.
 * 
 */
abstract class HessianRevReader<T> implements ObjectReader<T> {
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

    /**
     * Constructs a new {@code HessianRevReader}.
     */
    public HessianRevReader() {
        super();
    }

    @Override
    public final T read(ObjectId id, InputStream rawData) throws IllegalArgumentException {
        Hessian2Input hin = new Hessian2Input(rawData);
        try {
            hin.startMessage();
            RevObject.TYPE type = RevObject.TYPE.valueOf(hin.readInt());
            T object = read(id, hin, type);
            // completeMessage is taking ~60% of the CPU time, yourkit java profiler says, which is
            // just too odd specially since we're reading here
            // hin.completeMessage();
            return object;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                hin.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    protected abstract T read(ObjectId id, Hessian2Input hin, RevObject.TYPE type)
            throws IOException;

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
        Envelope bbox = readBBox(hin);

        org.geogit.api.Node ref = org.geogit.api.Node.create(name, id, metadataId, type, bbox);

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
    protected @Nullable
    Envelope readBBox(Hessian2Input hin) throws IOException {
        double minx = hin.readDouble();
        if (Double.isNaN(minx))
            return null;

        double maxx = hin.readDouble();
        double miny = hin.readDouble();
        double maxy = hin.readDouble();

        Envelope bbox = new Envelope(minx, maxx, miny, maxy);
        return bbox;
    }

}
