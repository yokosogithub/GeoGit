/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Iterator;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

/**
 * Provides an interface for accessing and managing GeoGit revision trees.
 * 
 * @see NodeRef
 */
public interface RevTree extends RevObject {

    /**
     * Provides a definition of a {@code NULL} tree.
     */
    public static RevTree NULL = new RevTree() {

        /**
         * @return the {@code TREE} type
         */
        @Override
        public TYPE getType() {
            return TYPE.TREE;
        }

        /**
         * @return a {@code NULL} {@link ObjectId}
         */
        @Override
        public ObjectId getId() {
            return ObjectId.NULL;
        }

        // @Override
        // public BigInteger size() {
        // return BigInteger.ZERO;
        // }

        /**
         * @return nothing, this operation is unsupported on a {@code NULL} tree
         * @throws UnsupportedOperationException always
         */
        @Override
        public MutableTree mutable() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * @param filter unused for {@code NULL} tree
         * @return an empty iterator
         */
        @Override
        public Iterator<NodeRef> iterator(Predicate<NodeRef> filter) {
            return Iterators.emptyIterator();
        }

        /**
         * @return always true for {@code NULL} trees
         */
        @Override
        public boolean isNormalized() {
            return true;
        }

        /**
         * @param key unused
         * @return always {@link Optional#absent()} for {@code NULL} trees
         */
        @Override
        public Optional<NodeRef> get(String key) {
            return Optional.absent();
        }

        /**
         * Does nothing with provided tree visitors.
         * 
         * @param visitor unused
         */
        @Override
        public void accept(TreeVisitor visitor) {
            // nothing to do
        }
    };

    /**
     * Retrieves the {@link NodeRef} that matches the given key from the tree.
     * 
     * @param key the path of the node to get
     * @return an {@link Optional} of the node if it exists, or {@link Optional#absent()} if it
     *         wasn't found.
     */
    public abstract Optional<NodeRef> get(final String key);

    /**
     * Accepts the provided tree visitor, providing a way to perform various actions on leaves and
     * subtrees of this tree.
     * 
     * @param visitor the visitor to use
     */
    public abstract void accept(TreeVisitor visitor);

    // public abstract BigInteger size();

    /**
     * Provides an iterator to iterate through the {@link NodeRef}s of the tree, skipping nodes that
     * match the provided filter.
     * 
     * @param filter the filter to use
     * @return an iterator that iterates through nodes that do not get filtered
     */
    public abstract Iterator<NodeRef> iterator(Predicate<NodeRef> filter);

    /**
     * @return true if the tree is normalized, false otherwise
     */
    public abstract boolean isNormalized();

    /**
     * @return a mutable version of this tree.
     * @throws UnsupportedOperationException if this tree cannot be mutable
     */
    public abstract MutableTree mutable() throws UnsupportedOperationException;
}