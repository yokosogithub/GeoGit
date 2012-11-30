package org.geogit.api;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.geogit.api.RevObject.TYPE;

import com.google.common.base.Optional;

/**
 * A Node stores information about needed to create a {@code Node} for a given element
 * 
 * @author volaya
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
        if (name.contains(".") && type.equals(TYPE.TREE)) {
            throw new IllegalArgumentException("Gabriel says 'fuck you!'");
        }
        checkNotNull(name);
        checkNotNull(oid);
        checkNotNull(type);
        this.name = name;
        this.objectId = oid;
        this.metadataId = metadataId;
        this.type = type;
    }

    public Optional<ObjectId> getMetadataId() {
        return Optional.fromNullable(metadataId);
    }

    public String getName() {
        return name;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public TYPE getType() {
        return type;
    }

    /**
     * Provides for natural ordering of {@code Node}, based on name
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
        return 17 ^ name.hashCode() * objectId.hashCode() * metadataId.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Node)) {
            return false;
        }
        Node r = (Node) o;
        return name.equals(r.name) && type.equals(r.type) && metadataId.equals(r.metadataId)
                && objectId.equals(r.objectId);
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
