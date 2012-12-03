/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * Base object type accessed during revision walking.
 * 
 * @see RevCommit
 * @see RevTree
 * @see RevFeature
 * @see RevTag
 */
public interface RevObject {

    /**
     * {@code RevObject} types enumeration.
     */
    public static enum TYPE {
        COMMIT {
            @Override
            public int value() {
                return 0;
            }
        },
        TREE {
            @Override
            public int value() {
                return 1;
            }
        },
        FEATURE {
            @Override
            public int value() {
                return 2;
            }
        },
        TAG {
            @Override
            public int value() {
                return 3;
            }
        },
        FEATURETYPE {
            @Override
            public int value() {
                return 4;
            }
        };

        public abstract int value();

        public static TYPE valueOf(final int value) {
            return TYPE.values()[value];
        }
    }

    /**
     * @return the object type of this object
     */
    public TYPE getType();

    /**
     * Get the name of this object.
     * 
     * @return unique hash of this object.
     */
    public ObjectId getId();

    /**
     * Equality is based on id
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o);
}
