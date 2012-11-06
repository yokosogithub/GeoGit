/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.hessian.HessianFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Provides an interface for accessing and managing GeoGit revision trees.
 * 
 * @see NodeRef
 */
public interface RevTree extends RevObject {

    /**
     * The canonical max size of a tree, hard limit, can't be changed or would affect the hash of
     * trees
     * 
     * @todo evaluate what a good compromise would be re memory usage/speed. So far 512 seems like a
     *       good compromise with an iteration throughput of 300K/s and random lookup of 50K/s on an
     *       Asus Zenbook UX31A. A value of 256 shields significantly lower throughput and a higher
     *       one (like 4096) no significant improvement
     */
    public static final int NORMALIZED_SIZE_LIMIT = 512;

    public static RevTree EMPTY = new RevTree() {

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

        @Override
        public RevTreeBuilder builder(ObjectDatabase target) {
            return new RevTreeBuilder(target, new HessianFactory());
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Optional<ImmutableList<NodeRef>> children() {
            return Optional.absent();
        }

        @Override
        public Optional<ImmutableSortedMap<Integer, ObjectId>> buckets() {
            return Optional.absent();
        }
    };

    public boolean isEmpty();

    public Optional<ImmutableList<NodeRef>> children();

    public Optional<ImmutableSortedMap<Integer, ObjectId>> buckets();

    public RevTreeBuilder builder(ObjectDatabase target);
}
