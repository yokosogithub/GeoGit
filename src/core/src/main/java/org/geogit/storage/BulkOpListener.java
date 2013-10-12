/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;

public abstract class BulkOpListener {

    public static final BulkOpListener NOOP_LISTENER = new BulkOpListener() {
    };

    /**
     * Signals each object found at {@link ObjectDatabase#getAll(Iterable, BulkOpListener)}
     * 
     * @param object the object found
     * @param storageSizeBytes <b>optional</b> the object storage size, if known.
     */
    public void found(RevObject object, @Nullable Integer storageSizeBytes) {
        // no-op
    }

    /**
     * Signals each object successfully inserted at
     * {@link ObjectDatabase#putAll(java.util.Iterator, BulkOpListener)} (e.g. objects that already
     * exists will not cause this method to be called)
     * 
     * @param object the object inserted
     * @param storageSizeBytes <b>optional</b> the object storage size, if known.
     */
    public void inserted(RevObject object, @Nullable Integer storageSizeBytes) {
        // no-op
    }

    /**
     * Signals each object succcessfully deleted at
     * {@link ObjectDatabase#deleteAll(java.util.Iterator, BulkOpListener)}
     * 
     * @param id the identifier of the deleted object
     */
    public void deleted(ObjectId id) {
        // no-op
    }

    /**
     * Signals each object not found in the database at either
     * {@link ObjectDatabase#getAll(Iterable, BulkOpListener)} or
     * {@link ObjectDatabase#deleteAll(java.util.Iterator, BulkOpListener)}.
     * 
     * @param id the identifier of the object not found in the database
     */
    public void notFound(ObjectId id) {
        // no-op
    }

    /**
     * Returns a composite listener that dispatches each signal to both listeners
     */
    public BulkOpListener composite(final BulkOpListener b1, final BulkOpListener b2) {
        if (b1 == NOOP_LISTENER) {
            return b2;
        }
        if (b2 == NOOP_LISTENER) {
            return b1;
        }
        return new BulkOpListener() {
            @Override
            public void found(RevObject object, Integer storageSizeBytes) {
                b1.found(object, storageSizeBytes);
                b2.found(object, storageSizeBytes);
            }

            @Override
            public void inserted(RevObject object, Integer storageSizeBytes) {
                b1.inserted(object, storageSizeBytes);
                b2.inserted(object, storageSizeBytes);
            }

            @Override
            public void deleted(ObjectId id) {
                b1.deleted(id);
                b2.deleted(id);
            }

            @Override
            public void notFound(ObjectId id) {
                b1.notFound(id);
                b2.notFound(id);
            }
        };
    }
}