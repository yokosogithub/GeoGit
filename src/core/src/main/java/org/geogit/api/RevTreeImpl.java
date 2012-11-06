/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import org.geogit.storage.NodeRefStorageOrder;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.hessian.HessianFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;

/**
 *
 */
public class RevTreeImpl extends AbstractRevObject implements RevTree {

    private static final class LeafTree extends RevTreeImpl {

        private final Optional<ImmutableList<NodeRef>> children;

        public LeafTree(ObjectId id, ImmutableList<NodeRef> childrenMap) {
            super(id);
            this.children = Optional.of(childrenMap);
        }

        @Override
        public Optional<ImmutableList<NodeRef>> children() {
            return children;
        }
    }

    private static final class NodeTree extends RevTreeImpl {

        private final Optional<ImmutableSortedMap<Integer, ObjectId>> buckets;

        public NodeTree(final ObjectId id, final ImmutableSortedMap<Integer, ObjectId> innerTrees) {
            super(id);
            this.buckets = Optional.of(innerTrees);
        }

        @Override
        public Optional<ImmutableSortedMap<Integer, ObjectId>> buckets() {
            return buckets;
        }
    }

    private RevTreeImpl(ObjectId id) {
        super(id);
    }

    @Override
    public boolean isEmpty() {
        return children().isPresent() ? children().get().isEmpty()
                : (buckets().isPresent() ? buckets().get().isEmpty() : true);
    }

    @Override
    public Optional<ImmutableList<NodeRef>> children() {
        return Optional.absent();
    }

    @Override
    public Optional<ImmutableSortedMap<Integer, ObjectId>> buckets() {
        return Optional.absent();
    }

    public static RevTreeImpl createLeafTree(ObjectId id, ImmutableList<NodeRef> children) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(children);
        return new LeafTree(id, children);
    }

    public static RevTreeImpl createLeafTree(ObjectId id, Collection<NodeRef> children) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(children);

        TreeSet<NodeRef> set = Sets.newTreeSet(new NodeRefStorageOrder());
        set.addAll(children);
        return createLeafTree(id, ImmutableList.copyOf(set));
    }

    public static RevTreeImpl createNodeTree(ObjectId id, Map<Integer, ObjectId> bucketTrees) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(bucketTrees);
        ImmutableSortedMap<Integer, ObjectId> innerTrees = ImmutableSortedMap.copyOf(bucketTrees);

        return new NodeTree(id, innerTrees);
    }

    static RevTreeImpl create(ObjectId id, RevTree unidentified) {
        if (unidentified.buckets().isPresent()) {
            return new NodeTree(id, unidentified.buckets().get());
        }
        ImmutableList<NodeRef> children = ImmutableList.of();
        if (unidentified.children().isPresent()) {
            children = unidentified.children().get();
        }
        return new LeafTree(id, children);
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
}
