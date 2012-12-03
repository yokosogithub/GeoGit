/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

/**
 * This enum describes what is encoded in a blob.
 * 
 */
enum BlobType {
    /**
     * Blob encodes a feature object
     */
    FEATURE(0),
    /**
     * Blob encodes a RevTree
     */
    REVTREE(1),
    /**
     * Blob encodes a Commit
     */
    COMMIT(2),
    /**
     * Blob encodes a Commit
     */
    FEATURETYPE(3);

    private int value;

    private BlobType(int value) {
        this.value = value;
    }

    /**
     * @return the {@code int} value of the enumeration
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Determines the {@code BlobType} given its integer value.
     * 
     * @param value The value of the desired {@code BlobType}
     * @return The correct {@code BlobType} for the value, or null if none is found.
     */
    public static BlobType fromValue(int value) {
        for (BlobType type : BlobType.values()) {
            if (type.value == value)
                return type;
        }
        return null;
    }
}
