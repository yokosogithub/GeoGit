/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import com.google.common.base.Optional;

/**
 * A Mutable version of RevTree.
 */
public interface MutableTree extends RevTree {

    /**
     * Adds a NodeRef to the tree.
     * 
     * @param ref the NodeRef to add
     * @see NodeRef
     */
    public abstract void put(final NodeRef ref);

    /**
     * Removes the NodeRef that matches the given key.
     * 
     * @param key the NodeRef to remove
     * @return an optional of the NodeRef if it was removed, or {@link Optional#absent()} if it
     *         wasn't found
     */
    public abstract Optional<NodeRef> remove(final String key);

    /**
     * Normalizes the tree.
     */
    public abstract void normalize();

}
