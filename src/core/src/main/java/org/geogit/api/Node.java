/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.geogit.api.RevObject.TYPE;

import com.google.common.base.Optional;

/**
 * An identifier->object id mapping for an object
 * 
 */
public class Node implements Comparable<Node> {

    /**
     * The name of the element
     */
    private String name;

    /**
     * Optional ID corresponding to metadata for the element
     */
    @Nullable
    private ObjectId metadataId;

    /**
     * The element type
     */
    private TYPE type;

    /**
     * Id of the object this ref points to
     */
    private ObjectId objectId;

    public Node(final String name, final ObjectId oid, final ObjectId metadataId,
            final RevObject.TYPE type) {
        checkNotNull(name);
        checkNotNull(oid);
        checkNotNull(type);
        this.name = name;
        this.objectId = oid;
        this.metadataId = metadataId.isNull() ? null : metadataId;
        this.type = type;
    }

    public Optional<ObjectId> getMetadataId() {
        return Optional.fromNullable(metadataId);
    }

    /**
     * @return the name of the {@link RevObject} this node points to
     */
    public String getName() {
        return name;
    }

    /**
     * @return the id of the {@link RevObject} this Node points to
     */
    public ObjectId getObjectId() {
        return objectId;
    }

    /**
     * @return the type of {@link RevObject} this node points to
     */
    public TYPE getType() {
        return type;
    }

    /**
     * Provides for natural ordering of {@code Node}, based on {@link #getName() name}
     */
    @Override
    public int compareTo(Node o) {
        return name.compareTo(o.getName());
    }

    /**
     * Hash code is based on name and object id
     */
    @Override
    public int hashCode() {
        return 17 ^ type.hashCode() * name.hashCode() * objectId.hashCode();
    }

    /**
     * Equality check based on {@link #getName() name}, {@link #getType() type}, and
     * {@link #getObjectId() objectId}; {@link #getMetadataId()} is NOT part of the equality check.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Node)) {
            return false;
        }
        Node r = (Node) o;
        return name.equals(r.name) && type.equals(r.type) && objectId.equals(r.objectId);
    }

    /**
     * @return the Node represented as a readable string.
     */
    @Override
    public String toString() {
        return new StringBuilder("Node").append('[').append(getName()).append(" -> ")
                .append(getObjectId()).append(']').toString();
    }

}
