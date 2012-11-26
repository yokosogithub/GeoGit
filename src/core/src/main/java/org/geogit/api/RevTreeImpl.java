/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.geogit.storage.NodeRefStorageOrder;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.hessian.HessianFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 *
 */
public abstract class RevTreeImpl extends AbstractRevObject implements RevTree {

    private static final class LeafTree extends RevTreeImpl {

        private final Optional<ImmutableList<NodeRef>> features;

        private final Optional<ImmutableList<NodeRef>> trees;

        public LeafTree(final ObjectId id, final long size,
                final Optional<ImmutableList<NodeRef>> features,
                Optional<ImmutableList<NodeRef>> trees) {
            super(id, size);
            this.features = features;
            this.trees = trees;
        }

        @Override
        public Optional<ImmutableList<NodeRef>> features() {
            return features;
        }

        @Override
        public Optional<ImmutableList<NodeRef>> trees() {
            return trees;
        }

        @Override
        public final boolean isEmpty() {
            return features.isPresent() ? features.get().isEmpty() : (trees.isPresent() ? trees
                    .get().isEmpty() : true);
        }
    }

    private static final class NodeTree extends RevTreeImpl {

        private final Optional<ImmutableSortedMap<Integer, ObjectId>> buckets;

        public NodeTree(final ObjectId id, final long size,
                final ImmutableSortedMap<Integer, ObjectId> innerTrees) {
            super(id, size);
            this.buckets = Optional.of(innerTrees);
        }

        @Override
        public Optional<ImmutableSortedMap<Integer, ObjectId>> buckets() {
            return buckets;
        }

        @Override
        public final boolean isEmpty() {
            return buckets().isPresent() ? buckets().get().isEmpty() : true;
        }
    }

    private final long size;

    private RevTreeImpl(ObjectId id, long size) {
        super(id);
        this.size = size;
    }

    @Override
    public final long size() {
        return size;
    }

    @Override
    public Optional<ImmutableList<NodeRef>> features() {
        return Optional.absent();
    }

    @Override
    public Optional<ImmutableList<NodeRef>> trees() {
        return Optional.absent();
    }

    @Override
    public Optional<ImmutableSortedMap<Integer, ObjectId>> buckets() {
        return Optional.absent();
    }

    public static RevTreeImpl createLeafTree(ObjectId id, long size,
            ImmutableList<NodeRef> features, ImmutableList<NodeRef> trees) {

        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(features);
        Preconditions.checkNotNull(trees);

        Optional<ImmutableList<NodeRef>> f = Optional.absent();
        Optional<ImmutableList<NodeRef>> t = Optional.absent();
        if (!features.isEmpty()) {
            f = Optional.of(features);
        }
        if (!trees.isEmpty()) {
            t = Optional.of(trees);
        }
        return new LeafTree(id, size, f, t);
    }

    public static RevTreeImpl createLeafTree(ObjectId id, long size, Collection<NodeRef> features,
            Collection<NodeRef> trees) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(features);

        ImmutableList<NodeRef> featuresList = ImmutableList.of();
        ImmutableList<NodeRef> treesList = ImmutableList.of();

        if (!features.isEmpty()) {
            TreeSet<NodeRef> featureSet = Sets.newTreeSet(new NodeRefStorageOrder());
            featureSet.addAll(features);
            featuresList = ImmutableList.copyOf(featureSet);
        }
        if (!trees.isEmpty()) {
            TreeSet<NodeRef> treeSet = Sets.newTreeSet(new NodeRefStorageOrder());
            treeSet.addAll(trees);
            treesList = ImmutableList.copyOf(treeSet);
        }
        return createLeafTree(id, size, featuresList, treesList);
    }

    public static RevTreeImpl createNodeTree(ObjectId id, long size,
            Map<Integer, ObjectId> bucketTrees) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(bucketTrees);
        ImmutableSortedMap<Integer, ObjectId> innerTrees = ImmutableSortedMap.copyOf(bucketTrees);

        return new NodeTree(id, size, innerTrees);
    }

    static RevTreeImpl create(ObjectId id, long size, RevTree unidentified) {
        if (unidentified.buckets().isPresent()) {
            return new NodeTree(id, size, unidentified.buckets().get());
        }
        final Optional<ImmutableList<NodeRef>> features;
        if (unidentified.features().isPresent()) {
            features = Optional.of(unidentified.features().get());
        } else {
            features = Optional.absent();
        }
        final Optional<ImmutableList<NodeRef>> trees;
        if (unidentified.trees().isPresent()) {
            trees = Optional.of(unidentified.trees().get());
        } else {
            trees = Optional.absent();
        }
        return new LeafTree(id, size, features, trees);
    }

    @Override
    public TYPE getType() {
        return TYPE.TREE;
    }

    @Override
    public RevTreeBuilder builder(ObjectDatabase target) {
        // TODO: new HessianFactory() will be removed once SerializationFactory is known to
        // ObjectDatabase
        return new RevTreeBuilder(target, new HessianFactory(), this);
    }

    @Override
    public Iterator<NodeRef> children() {
        Preconditions.checkState(!buckets().isPresent());
        final ImmutableList<NodeRef> empty = ImmutableList.of();
        return Iterators.concat(trees().or(empty).iterator(), features().or(empty).iterator());
    }
}
