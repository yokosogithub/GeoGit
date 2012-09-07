/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 */
public class NodeRef implements Comparable<NodeRef> {

    private String name;

    private RevObject.TYPE type;

    private ObjectId objectId;

    public NodeRef(final String name, final ObjectId oid, final RevObject.TYPE type) {
        checkNotNull(name);
        checkNotNull(oid);
        checkNotNull(type);
        this.name = name;
        this.objectId = oid;
        this.type = type;
    }

    /**
     * The name of this edge
     */
    public String getName() {
        return name;
    }

    /**
     * The id of the object this edge points to
     */
    public ObjectId getObjectId() {
        return objectId;
    }

    public RevObject.TYPE getType() {
        return type;
    }

    /**
     * Tests equality over another {@code NodeRef}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeRef)) {
            return false;
        }
        NodeRef r = (NodeRef) o;
        return name.equals(r.getName()) && type.equals(r.getType())
                && objectId.equals(r.getObjectId());
    }

    /**
     * Hash code is based on name and object id
     */
    @Override
    public int hashCode() {
        return name.hashCode() * objectId.hashCode();
    }

    /**
     * Provides for natural ordering of {@code NodeRef}, based on name
     */
    @Override
    public int compareTo(NodeRef o) {
        return name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return new StringBuilder("Ref").append('[').append(name).append(" -> ").append(objectId)
                .append(']').toString();
    }
}