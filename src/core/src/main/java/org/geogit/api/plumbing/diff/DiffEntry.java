/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.meta.When;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.repository.SpatialOps;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Preconditions;

public class DiffEntry {

    public static enum ChangeType {
        /**
         * Add a new Feature
         */
        ADDED {
            @Override
            public int value() {
                return 0;
            }
        },

        /**
         * Modify an existing Feature
         */
        MODIFIED {
            @Override
            public int value() {
                return 1;
            }
        },

        /**
         * Delete an existing Feature
         */
        REMOVED {
            @Override
            public int value() {
                return 2;
            }
        };

        public abstract int value();

        public static ChangeType valueOf(int value) {
            // relying in the enum ordinal, beware
            return ChangeType.values()[value];
        }
    }

    private final NodeRef oldObject;

    private final NodeRef newObject;

    public DiffEntry(@Nonnull(when = When.MAYBE) NodeRef oldObject,
            @Nonnull(when = When.MAYBE) NodeRef newObject) {

        Preconditions.checkArgument(oldObject != null || newObject != null,
                "Either oldObject or newObject shall not be null");

        if (oldObject != null && oldObject.equals(newObject)) {
            throw new IllegalArgumentException(
                    "Trying to create a DiffEntry for the same object id, means the object didn't change: "
                            + oldObject.toString());
        }
        if (oldObject != null && newObject != null) {
            checkArgument(oldObject.getType().equals(newObject.getType()), String.format(
                    "Types don't match: %s : %s", oldObject.getType().toString(), newObject
                            .getType().toString()));
        }

        this.oldObject = oldObject;
        this.newObject = newObject;
    }

    /**
     * @return the id of the old version id of the object, or {@link ObjectId#NULL} if
     *         {@link #changeType()} is {@code ADD}
     */
    public ObjectId oldObjectId() {
        return oldObject == null ? ObjectId.NULL : oldObject.getObjectId();
    }

    /**
     * @return the old object, or {@code null} if {@link #changeType()} is {@code ADD}
     */
    public NodeRef getOldObject() {
        return oldObject;
    }

    /**
     * @return the id of the new version id of the object, or {@link ObjectId#NULL} if
     *         {@link #changeType()} is {@code DELETE}
     */
    public ObjectId newObjectId() {
        return newObject == null ? ObjectId.NULL : newObject.getObjectId();
    }

    /**
     * @return the id of the new version of the object, or {@code null} if {@link #changeType()} is
     *         {@code DELETE}
     */
    public NodeRef getNewObject() {
        return newObject;
    }

    /**
     * @return the type of change
     */
    public ChangeType changeType() {
        ChangeType type;
        if (oldObject == null || oldObject.getObjectId().isNull()) {
            type = ChangeType.ADDED;
        } else if (newObject == null || newObject.getObjectId().isNull()) {
            type = ChangeType.REMOVED;
        } else {
            type = ChangeType.MODIFIED;
        }

        return type;
    }

    /**
     * @return the affected geographic region of the change, may be {@code null}
     */
    public BoundingBox where() {
        BoundingBox bounds = SpatialOps.aggregatedBounds(oldObject, newObject);
        return bounds;
    }

    @Override
    public String toString() {
        return new StringBuilder(changeType().toString()).append(" [").append(oldObject)
                .append("] -> [").append(newObject).append("]").toString();
    }

    public @Nullable
    String oldPath() {
        return oldObject == null ? null : oldObject.getPath();
    }

    public @Nullable
    String newPath() {
        return newObject == null ? null : newObject.getPath();
    }
}
