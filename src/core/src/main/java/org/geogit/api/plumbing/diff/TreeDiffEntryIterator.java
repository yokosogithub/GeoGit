/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.ADDED;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.REMOVED;

import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DepthTreeIterator.Strategy;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.storage.NodeStorageOrder;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;

/**
 * Traverses the direct children iterators of both trees (fromTree and toTree) simultaneously. If
 * the current children is named the same for both iterators, finds out whether the two children are
 * changed. If the two elements of the current iteration are not the same, find out whether it's an
 * addition or a deletion; when the change is on a subtree, returns the subtree differences before
 * continuing with the own ones.
 */
class TreeDiffEntryIterator extends AbstractIterator<DiffEntry> {

    private final ObjectDatabase objectDb;

    private Iterator<DiffEntry> delegate;

    private final boolean reportTrees;

    /**
     * The {@link Strategy} used to iterate the two trees which tells whether to report or not tree
     * entries besides feature entries
     */
    private final Strategy strategy;

    public TreeDiffEntryIterator(@Nullable NodeRef oldTreeRef, @Nullable NodeRef newTreeRef,
            @Nullable final RevTree oldTree, @Nullable final RevTree newTree,
            final boolean reportTrees, final ObjectDatabase db) {

        checkArgument(oldTree != null || newTree != null);

        this.reportTrees = reportTrees;
        this.objectDb = db;

        if (reportTrees) {
            strategy = DepthTreeIterator.Strategy.RECURSIVE;
        } else {
            strategy = DepthTreeIterator.Strategy.RECURSIVE_FEATURES_ONLY;
        }

        if (oldTree == null || oldTree.isEmpty()) {
            delegate = addRemoveAll(newTreeRef, newTree, ADDED);
        } else if (newTree == null || newTree.isEmpty()) {
            delegate = addRemoveAll(oldTreeRef, oldTree, REMOVED);
        } else if (!oldTree.buckets().isPresent() && !newTree.buckets().isPresent()) {

            Iterator<NodeRef> left = new DepthTreeIterator(oldTreeRef.path(),
                    oldTreeRef.getMetadataId(), oldTree, db, DepthTreeIterator.Strategy.CHILDREN);

            Iterator<NodeRef> right = new DepthTreeIterator(newTreeRef.path(),
                    newTreeRef.getMetadataId(), newTree, db, DepthTreeIterator.Strategy.CHILDREN);

            delegate = new ChildrenChildrenDiff(left, right);
        } else if (oldTree.buckets().isPresent() && newTree.buckets().isPresent()) {
            delegate = new BucketBucketDiff(oldTreeRef, newTreeRef, oldTree.buckets().get(),
                    newTree.buckets().get());

        } else if (newTree.buckets().isPresent()) {
            checkState(!oldTree.buckets().isPresent());
            DepthTreeIterator left = new DepthTreeIterator(oldTreeRef.path(),
                    oldTreeRef.getMetadataId(), oldTree, objectDb, strategy);

            DepthTreeIterator rightIterator;
            rightIterator = new DepthTreeIterator(newTreeRef.path(), newTreeRef.getMetadataId(),
                    newTree, objectDb, strategy);
            delegate = new ChildrenChildrenDiff(left, rightIterator);

        } else {
            checkState(oldTree.buckets().isPresent());

            DepthTreeIterator right = new DepthTreeIterator(newTreeRef.path(),
                    newTreeRef.getMetadataId(), newTree, objectDb, strategy);

            DepthTreeIterator leftIterator;
            leftIterator = new DepthTreeIterator(oldTreeRef.path(), oldTreeRef.getMetadataId(),
                    oldTree, objectDb, strategy);
            delegate = new ChildrenChildrenDiff(leftIterator, right);
            // delegate = new BucketsChildrenDiff(left, right);
        }

        // if (reportTrees && !Objects.equal(oldTreeRef, newTreeRef)) {
        // DiffEntry treeEntry = new DiffEntry(oldTreeRef, newTreeRef);
        // delegate = Iterators.concat(Iterators.singletonIterator(treeEntry), delegate);
        // }

    }

    @Override
    protected DiffEntry computeNext() {
        if (delegate.hasNext()) {
            return delegate.next();
        }
        return endOfData();
    }

    private Iterator<DiffEntry> addRemoveAll(@Nullable final NodeRef treeRef, final RevTree tree,
            final ChangeType changeType) {
        DepthTreeIterator treeIterator;

        final String path = treeRef == null ? "" : treeRef.path();

        treeIterator = new DepthTreeIterator(path, treeRef.getMetadataId(), tree, objectDb,
                strategy);

        Iterator<DiffEntry> iterator = Iterators.transform(treeIterator, new RefToDiffEntry(
                changeType));

        if (reportTrees) {
            NodeRef oldTreeRef = ChangeType.ADDED.equals(changeType) ? null : treeRef;
            NodeRef newTreeRef = ChangeType.ADDED.equals(changeType) ? treeRef : null;
            DiffEntry treeEntry = new DiffEntry(oldTreeRef, newTreeRef);
            iterator = Iterators.concat(Iterators.singletonIterator(treeEntry), iterator);
        }
        return iterator;
    }

    /**
     * Compares the contents of two leaf trees and spits out the changes. The entries must be in
     * {@link NodeRef}'s {@link NodeStorageOrder storage order}.
     * 
     */
    private class ChildrenChildrenDiff extends AbstractIterator<DiffEntry> {

        private PeekingIterator<NodeRef> left;

        private PeekingIterator<NodeRef> right;

        private Ordering<Node> comparator;

        private @Nullable
        Iterator<DiffEntry> subtreeIterator;

        public ChildrenChildrenDiff(Iterator<NodeRef> left, Iterator<NodeRef> right) {

            this.left = Iterators.peekingIterator(left);
            this.right = Iterators.peekingIterator(right);
            this.comparator = new NodeStorageOrder();
        }

        @Override
        protected DiffEntry computeNext() {
            if (null != subtreeIterator) {
                if (subtreeIterator.hasNext()) {
                    return subtreeIterator.next();
                }
                subtreeIterator = null;
            }
            if (!(left.hasNext() || right.hasNext())) {
                return endOfData();
            }

            // use peek to glimpse over the next values without consuming the iterator
            NodeRef nextLeft = left.hasNext() ? left.peek() : null;
            NodeRef nextRight = right.hasNext() ? right.peek() : null;

            if (nextLeft == null) {
                nextRight = right.next();
            } else if (nextRight == null) {
                nextLeft = left.next();
            } else if (nextLeft.path().equals(nextRight.path())) {
                // same path, consume both
                nextLeft = left.next();
                nextRight = right.next();
                if (nextLeft.equals(nextRight)) {
                    // but not a diff
                    return computeNext();
                }
            } else if (comparator.min(nextLeft.getNode(), nextRight.getNode()) == nextLeft
                    .getNode()) {
                nextLeft = left.next();
                nextRight = null;
            } else {
                nextLeft = null;
                nextRight = right.next();
            }

            final boolean isSubtree = (nextLeft != null && nextLeft.getType() == TYPE.TREE)
                    || (nextRight != null && nextRight.getType() == TYPE.TREE);

            if (isSubtree) {
                this.subtreeIterator = resolveSubtreeIterator(nextLeft, nextRight);
                return computeNext();
            }

            DiffEntry entry = new DiffEntry(nextLeft, nextRight);
            return entry;
        }

        private Iterator<DiffEntry> resolveSubtreeIterator(@Nullable NodeRef nextLeft,
                @Nullable NodeRef nextRight) {

            checkArgument(nextLeft != null || nextRight != null);

            RevTree fromTree = resolveSubtree(nextLeft);
            RevTree toTree = resolveSubtree(nextRight);

            Iterator<DiffEntry> it;

            if (fromTree == null || fromTree.isEmpty()) {
                checkState(toTree != null);
                it = addRemoveAll(nextRight, toTree, ADDED);
            } else if (toTree == null || toTree.isEmpty()) {
                checkState(fromTree != null);
                it = addRemoveAll(nextLeft, fromTree, REMOVED);
            } else {
                it = new TreeDiffEntryIterator(nextLeft, nextRight, fromTree, toTree, reportTrees,
                        objectDb);
            }
            return it;
        }

        private @Nullable
        RevTree resolveSubtree(@Nullable NodeRef treeRef) {
            if (treeRef == null) {
                return null;
            }
            ObjectId id = treeRef.objectId();
            RevTree tree = objectDb.getTree(id);
            return tree;
        }
    }

    /**
     * Function that converts a single {@link Node} to an add or remove {@link DiffEntry}
     */
    private static class RefToDiffEntry implements Function<NodeRef, DiffEntry> {

        private ChangeType changeType;

        public RefToDiffEntry(ChangeType changeType) {
            Preconditions.checkArgument(ADDED.equals(changeType) || REMOVED.equals(changeType));
            this.changeType = changeType;
        }

        @Override
        public DiffEntry apply(final NodeRef ref) {
            if (ADDED.equals(changeType)) {
                return new DiffEntry(null, ref);
            }
            return new DiffEntry(ref, null);
        }

    }

    private class BucketBucketDiff extends AbstractIterator<DiffEntry> {

        /**
         * A multi-map of bucket/objectId where key is guaranteed to have two entries, the first one
         * for the left tree id and the second one for he right tree id
         */
        private final ListMultimap<Integer, Optional<Bucket>> leftRightBuckets;

        private final Iterator<Integer> combinedBuckets;

        private Iterator<DiffEntry> currentBucketIterator;

        private NodeRef leftRef;

        private NodeRef rightRef;

        public BucketBucketDiff(final NodeRef leftRef, final NodeRef rightRef,
                final ImmutableSortedMap<Integer, Bucket> left,
                final ImmutableSortedMap<Integer, Bucket> right) {

            this.leftRef = leftRef;
            this.rightRef = rightRef;
            int expectedKeys = left.size() + right.size();
            int expectedValuesPerKey = 2;
            leftRightBuckets = ArrayListMultimap.create(expectedKeys, expectedValuesPerKey);

            Set<Integer> buckets = Sets.newTreeSet(Sets.union(left.keySet(), right.keySet()));
            for (Integer bucket : buckets) {
                leftRightBuckets.put(bucket, Optional.fromNullable(left.get(bucket)));
                leftRightBuckets.put(bucket, Optional.fromNullable(right.get(bucket)));
            }
            this.combinedBuckets = leftRightBuckets.keySet().iterator();
        }

        @Override
        protected DiffEntry computeNext() {
            if (currentBucketIterator != null && currentBucketIterator.hasNext()) {
                return currentBucketIterator.next();
            }
            if (!combinedBuckets.hasNext()) {
                return endOfData();
            }

            final Integer bucket = combinedBuckets.next();
            final Bucket leftBucket = leftRightBuckets.get(bucket).get(0).orNull();
            final Bucket rightBucket = leftRightBuckets.get(bucket).get(1).orNull();
            final RevTree left = resolveTree(leftBucket);
            final RevTree right = resolveTree(rightBucket);

            // TODO******
            this.currentBucketIterator = new TreeDiffEntryIterator(leftRef, rightRef, left, right,
                    reportTrees, objectDb);
            return computeNext();
        }

        private RevTree resolveTree(@Nullable Bucket bucket) {
            if (bucket == null) {
                return null;
            }
            return objectDb.getTree(bucket.id());
        }
    }
}