/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.storage.NodeRefStorageOrder;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * A faster alternative to count the number of diffs between two trees than walking a
 * {@link DiffTreeWalk} iterator; doesn't support filtering, counts the total number of differences
 * between the two trees
 * <p>
 * TODO: add support for path filtering
 */
public class DiffCounter implements Supplier<Long> {

    @Nonnull
    private final RevTree fromRootTree;

    @Nonnull
    private final RevTree toRootTree;

    @Nonnull
    private ObjectDatabase objectDb;

    private ObjectSerialisingFactory serialFactory;

    public DiffCounter(final ObjectDatabase db, final RevTree fromRootTree,
            final RevTree toRootTree, final ObjectSerialisingFactory serialFactory) {
        Preconditions.checkNotNull(db);
        Preconditions.checkNotNull(fromRootTree);
        Preconditions.checkNotNull(toRootTree);
        Preconditions.checkNotNull(serialFactory);
        this.objectDb = db;
        this.fromRootTree = fromRootTree;
        this.toRootTree = toRootTree;
        this.serialFactory = serialFactory;
    }

    @Override
    public Long get() {

        RevTree oldTree = this.fromRootTree;
        RevTree newTree = this.toRootTree;

        return countDiffs(oldTree, newTree);
    }

    private Long countDiffs(ObjectId oldTreeId, ObjectId newTreeId) {
        RevTree leftTree = getTree(oldTreeId);
        RevTree rightTree = getTree(newTreeId);
        return countDiffs(leftTree, rightTree);
    }

    private long countDiffs(RevTree oldTree, RevTree newTree) {
        if (oldTree.getId().equals(newTree.getId())) {
            return 0L;
        } else if (newTree.isEmpty()) {
            return countOf(oldTree);
        } else if (oldTree.isEmpty()) {
            return countOf(newTree);
        }

        long count = 0L;

        final boolean childrenVsChildren = oldTree.children().isPresent()
                && newTree.children().isPresent();
        final boolean bucketsVsBuckets = oldTree.buckets().isPresent()
                && newTree.buckets().isPresent();

        if (childrenVsChildren) {
            ImmutableList<NodeRef> leftChildren = oldTree.children().get();
            ImmutableList<NodeRef> rightChildren = newTree.children().get();
            count = countChildrenDiffs(leftChildren, rightChildren);
        } else if (bucketsVsBuckets) {
            ImmutableSortedMap<Integer, ObjectId> leftBuckets = oldTree.buckets().get();
            ImmutableSortedMap<Integer, ObjectId> rightBuckets = newTree.buckets().get();
            count = countBucketDiffs(leftBuckets, rightBuckets);
        } else {
            // get the children and buckets from the respective trees, order doesn't matter as we're
            // counting diffs
            ImmutableSortedMap<Integer, ObjectId> buckets;
            ImmutableList<NodeRef> children;

            buckets = oldTree.buckets().isPresent() ? oldTree.buckets().get() : newTree.buckets()
                    .get();
            children = oldTree.children().isPresent() ? oldTree.children().get() : newTree
                    .children().get();

            count = countBucketsChildren(buckets, children);
        }

        return count;
    }

    /**
     * Handles the case where one version of a tree has so few nodes that they all fit in its
     * {@link RevTree#children()} list, but the other version of the tree has more nodes so its
     * split into {@link RevTree#buckets()}. Nonetheless, it doesn't mean that the smaller tree
     */
    private long countBucketsChildren(ImmutableSortedMap<Integer, ObjectId> buckets,
            ImmutableList<NodeRef> children) {

        final NodeRefStorageOrder refOrder = new NodeRefStorageOrder();
        final int bucketDepth = 0; // start at depth 0
        return countBucketsChildren(buckets, children, refOrder, bucketDepth);
    }

    private long countBucketsChildren(ImmutableSortedMap<Integer, ObjectId> buckets,
            Collection<NodeRef> children, final NodeRefStorageOrder refOrder, final int depth) {

        final SortedSetMultimap<Integer, NodeRef> childrenByBucket;
        {
            childrenByBucket = TreeMultimap.create();
            for (NodeRef ref : children) {
                Integer bucket = refOrder.bucket(ref, depth);
                childrenByBucket.put(bucket, ref);
            }
        }

        long count = 0;

        {// count full size of all buckets for which no chilren falls into
            final Set<Integer> loneleyBuckets = Sets.difference(buckets.keySet(),
                    childrenByBucket.keySet());
            for (Integer bucket : loneleyBuckets) {
                ObjectId bucketId = buckets.get(bucket);
                count += sizeOfTree(bucketId);
            }
        }
        {// count the full size of all children whose buckets don't exist on the buckets tree
            final Set<Integer> nonExistingBuckets = Sets.difference(childrenByBucket.keySet(),
                    buckets.keySet());

            for (Integer bucket : nonExistingBuckets) {
                SortedSet<NodeRef> refs = childrenByBucket.get(bucket);
                count += aggregateSize(refs);
            }
        }

        // find the number of diffs of the intersection
        final Set<Integer> commonBuckets = Sets.intersection(buckets.keySet(),
                childrenByBucket.keySet());
        for (Integer bucket : commonBuckets) {

            final Set<NodeRef> refs = childrenByBucket.get(bucket);

            final ObjectId bucketId = buckets.get(bucket);
            final RevTree bucketTree = getTree(bucketId);

            if (bucketTree.isEmpty()) {
                // unlikely
                count += aggregateSize(refs);
            } else if (bucketTree.children().isPresent()) {
                TreeSet<NodeRef> sortedRefs = Sets.newTreeSet(refOrder);
                sortedRefs.addAll(refs);
                ImmutableList<NodeRef> rightChildren = ImmutableList.copyOf(sortedRefs);
                count += countChildrenDiffs(bucketTree.children().get(), rightChildren);
            } else {
                final int deeperBucketsDepth = depth + 1;
                final ImmutableSortedMap<Integer, ObjectId> deeperBuckets;
                deeperBuckets = bucketTree.buckets().get();
                count += countBucketsChildren(deeperBuckets, refs, refOrder, deeperBucketsDepth);
            }
        }

        return count;
    }

    /**
     * Counts the number of differences between two trees that contain {@link RevTree#buckets()
     * buckets} instead of direct {@link RevTree#children() children}
     */
    private long countBucketDiffs(ImmutableSortedMap<Integer, ObjectId> leftBuckets,
            ImmutableSortedMap<Integer, ObjectId> rightBuckets) {

        long count = 0;
        final Set<Integer> bucketIds = Sets.union(leftBuckets.keySet(), rightBuckets.keySet());

        ObjectId leftTreeId;
        ObjectId rightTreeId;

        for (Integer bucketId : bucketIds) {
            leftTreeId = leftBuckets.get(bucketId);
            rightTreeId = rightBuckets.get(bucketId);

            if (leftTreeId == null || rightTreeId == null) {
                count += sizeOfTree(leftTreeId == null ? rightTreeId : leftTreeId);
            } else {
                count += countDiffs(leftTreeId, rightTreeId);
            }
        }
        return count;
    }

    private long countChildrenDiffs(ImmutableList<NodeRef> leftChildren,
            ImmutableList<NodeRef> rightChildren) {

        final Ordering<NodeRef> storageOrder = new NodeRefStorageOrder();

        long count = 0;

        PeekingIterator<NodeRef> left = Iterators.peekingIterator(leftChildren.iterator());
        PeekingIterator<NodeRef> right = Iterators.peekingIterator(rightChildren.iterator());

        while (left.hasNext() && right.hasNext()) {
            NodeRef peekLeft = left.peek();
            NodeRef peekRight = right.peek();

            if (0 == storageOrder.compare(peekLeft, peekRight)) {
                // same path, consume both
                peekLeft = left.next();
                peekRight = right.next();
                if (!peekLeft.getObjectId().equals(peekRight.getObjectId())) {
                    // find the diffs between these two specific refs
                    if (RevObject.TYPE.FEATURE.equals(peekLeft.getType())) {
                        checkState(RevObject.TYPE.FEATURE.equals(peekRight.getType()));
                        count++;
                    } else {
                        checkState(RevObject.TYPE.TREE.equals(peekLeft.getType()));
                        checkState(RevObject.TYPE.TREE.equals(peekRight.getType()));
                        ObjectId leftTreeId = peekLeft.getObjectId();
                        ObjectId rightTreeId = peekRight.getObjectId();
                        count += countDiffs(leftTreeId, rightTreeId);
                    }
                }
            } else if (peekLeft == storageOrder.min(peekLeft, peekRight)) {
                peekLeft = left.next();// consume only the left value
                count += aggregateSize(ImmutableList.of(peekLeft));
            } else {
                peekRight = right.next();// consume only the right value
                count += aggregateSize(ImmutableList.of(peekRight));
            }
        }

        if (left.hasNext()) {
            count += countRemaining(left);
        } else if (right.hasNext()) {
            count += countRemaining(right);
        }
        return count;
    }

    private long countRemaining(Iterator<NodeRef> remaining) {
        ArrayList<NodeRef> iterable = Lists.newArrayList(remaining);
        return aggregateSize(iterable);
    }

    private long sizeOfTree(ObjectId treeId) {
        RevTree tree = getTree(treeId);
        return countOf(tree);
    }

    private RevTree getTree(ObjectId treeId) {
        if (treeId.isNull()) {
            return RevTree.EMPTY;
        }
        RevTree tree = objectDb.get(treeId, serialFactory.createRevTreeReader());
        return tree;
    }

    /**
     * @return the total size of {@code tree}
     */
    private long countOf(RevTree tree) {
        return tree.size();
    }

    private long aggregateSize(Iterable<NodeRef> children) {
        long size = 0;
        for (NodeRef ref : children) {
            if (RevObject.TYPE.FEATURE.equals(ref.getType())) {
                size++;
            } else if (RevObject.TYPE.TREE.equals(ref.getType())) {
                ObjectId treeId = ref.getObjectId();
                size += sizeOfTree(treeId);
            }
        }
        return size;
    }

}
