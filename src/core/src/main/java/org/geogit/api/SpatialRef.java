/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.api.RevObject.TYPE;
import org.opengis.geometry.BoundingBox;

/**
 * A NodeRef with spatial hints.
 */
public class SpatialRef extends NodeRef {

    private BoundingBox bounds;

    /**
     * Constructs a new {@code SpatialRef} with the given parameters.
     * 
     * @param path the path of the node
     * @param oid the id of the object at the node
     * @param metadataId id of the object that contains metadata for this node
     * @param type the type of object being stored
     * @param bounds the bounds of the node
     */
    public SpatialRef(String path, ObjectId oid, ObjectId metadataId, TYPE type, BoundingBox bounds) {
        super(path, oid, metadataId, type);
        this.bounds = bounds;
    }

    /**
     * @return the bounds of this node
     */
    public BoundingBox getBounds() {
        return bounds;
    }
}
