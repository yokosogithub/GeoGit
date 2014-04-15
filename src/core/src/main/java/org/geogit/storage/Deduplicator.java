/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.List;

import org.geogit.api.ObjectId;

/**
 * A deduplicator identifies duplicates in a stream of ObjectIds.
 */
public interface Deduplicator {
    /**
     * Tests an objectid for being a duplicate.  
     * This method does not alter the state of the deduplicator (an unseen
     * objectid is still unseen after isDuplicate has returned false.)
     */
    boolean isDuplicate(ObjectId id);

    /**
     * Marks an objectid as being a duplicate.
     * This method changes the state of the deduplicator; after calling it on
     * an object that object will be considered visited.  The return value
     * indicates whether or not the objectid was already a duplicate before
     * calling.
     */
    boolean visit(ObjectId id);

    /**
     * Convenience method for filtering out duplicate objectids from a list
     */
    void removeDuplicates(List<ObjectId> ids);

    /**
     * Clear out the memory of this deduplicator so that no objects are considered visited
     */
    void reset();

    /**
     * Release any external resources used to back this deduplicator, and invalidate the deduplicator.
     * After release() has been called, the deduplicator should no longer be used.
     */
    void release();
}
