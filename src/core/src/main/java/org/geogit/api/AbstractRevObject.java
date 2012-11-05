/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * Base object type accessed during revision walking.
 * 
 * @author groldan
 * @see RevCommit
 * @see RevTree
 * @see RevFeature
 * @see RevTag
 */
public abstract class AbstractRevObject implements RevObject {
    private final ObjectId id;

    private final TYPE type;

    /**
     * Constructs a new AbstractRevObject with the given id and type
     * 
     * @param id ObjectId of this RevObject
     * @param type the type of this RevObject
     */
    public AbstractRevObject(final ObjectId id, final TYPE type) {
        this.id = id;
        this.type = type;
    }

    /**
     * @return the type for this object
     * @see RevObject.TYPE
     */
    public final TYPE getType() {
        return type;
    }

    /**
     * Get the name of this object.
     * 
     * @return unique hash of this object.
     */
    public final ObjectId getId() {
        return id;
    }

    /**
     * Equality is based on id
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AbstractRevObject)) {
            return false;
        }
        return id.equals(((AbstractRevObject) o).getId());
    }
}
