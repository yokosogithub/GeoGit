/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;

public abstract class BulkOpListener {

    public static final BulkOpListener NOOP_LISTENER = new BulkOpListener() {
    };

    /**
     * Signals each object found at {@link ObjectDatabase#getAll(Iterable, BulkOpListener)} as the
     * returned iterator is traversed, or each object not inserted at
     * {@link ObjectDatabase#putAll(java.util.Iterator, BulkOpListener)} because it already exists.
     * 
     * @param object the object found
     * @param storageSizeBytes <b>optional</b> the object storage size, if known.
     */
    public void found(ObjectId object, @Nullable Integer storageSizeBytes) {
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
    public void inserted(ObjectId object, @Nullable Integer storageSizeBytes) {
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

    public static CountingListener newCountingListener() {
        return new CountingListener();
    }

    /**
     * Returns a composite listener that dispatches each signal to both listeners
     */
    public static BulkOpListener composite(final BulkOpListener b1, final BulkOpListener b2) {
        if (b1 == NOOP_LISTENER) {
            return b2;
        }
        if (b2 == NOOP_LISTENER) {
            return b1;
        }
        return new BulkOpListener() {
            @Override
            public void found(ObjectId object, Integer storageSizeBytes) {
                b1.found(object, storageSizeBytes);
                b2.found(object, storageSizeBytes);
            }

            @Override
            public void inserted(ObjectId object, Integer storageSizeBytes) {
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

    public static class ForwardingListener extends BulkOpListener {

        private BulkOpListener target;

        public ForwardingListener(BulkOpListener target) {
            this.target = target;
        }

        @Override
        public void found(ObjectId object, @Nullable Integer storageSizeBytes) {
            target.found(object, storageSizeBytes);
        }

        public void inserted(ObjectId object, @Nullable Integer storageSizeBytes) {
            target.inserted(object, storageSizeBytes);
        }

        @Override
        public void deleted(ObjectId id) {
            target.deleted(id);
        }

        @Override
        public void notFound(ObjectId id) {
            target.notFound(id);
        }
    }

    public static class CountingListener extends BulkOpListener {
        private AtomicInteger found = new AtomicInteger();

        private AtomicInteger inserted = new AtomicInteger();

        private AtomicInteger deleted = new AtomicInteger();

        private AtomicInteger notFound = new AtomicInteger();

        @Override
        public void found(ObjectId object, @Nullable Integer storageSizeBytes) {
            found.incrementAndGet();
        }

        @Override
        public void inserted(ObjectId object, @Nullable Integer storageSizeBytes) {
            inserted.incrementAndGet();
        }

        @Override
        public void deleted(ObjectId id) {
            deleted.incrementAndGet();
        }

        @Override
        public void notFound(ObjectId id) {
            notFound.incrementAndGet();
        }

        public int found() {
            return found.get();
        }

        public int deleted() {
            return deleted.get();
        }

        public int inserted() {
            return inserted.get();
        }

        public int notFound() {
            return notFound.get();
        }
    }
}