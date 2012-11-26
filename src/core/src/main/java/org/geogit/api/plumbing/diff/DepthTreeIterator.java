/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.hessian.HessianFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

/**
 * An iterator over a {@link RevTree} that can return different results depending on the
 * {@link #Strategy} given;
 */
public class DepthTreeIterator extends AbstractIterator<NodeRef> {
    public enum Strategy {
        /**
         * Default strategy, list the all direct child entries of a tree, no recursion
         */
        CHILDREN,
        /**
         * List only the direct child entries of a tree that are of type FEATURE
         */
        FEATURES_ONLY,
        /**
         * List only the direct child entries of a tree that are of type TREE
         */
        TREES_ONLY,
        /**
         * Recursively list the contents of a tree in depth-first order, including both TREE and
         * FEATURE entries
         */
        RECURSIVE,
        /**
         * Recursively list the contents of a tree in depth-first order, but do not report TREE
         * entries, only FEATURE ones
         */
        RECURSIVE_FEATURES_ONLY,
        /**
         * Recursively list the contents of a tree in depth-first order, but do not report TREE
         * entries, only FEATURE ones
         */
        RECURSIVE_TREES_ONLY
    }

    private RevTree tree;

    private Iterator<NodeRef> iterator;

    private ObjectDatabase source;

    private ObjectSerialisingFactory serialFactory = new HessianFactory();

    private Strategy strategy;

    public DepthTreeIterator(RevTree tree, ObjectDatabase source, Strategy strategy) {
        checkNotNull(tree);
        checkNotNull(source);
        checkNotNull(strategy);

        this.tree = tree;
        this.source = source;
        this.strategy = strategy;
        switch (strategy) {
        case CHILDREN:
            iterator = new Children(tree);
            break;
        case FEATURES_ONLY:
            iterator = new Features(tree);
            break;
        case TREES_ONLY:
            iterator = new Trees(tree);
            break;
        case RECURSIVE:
            iterator = new Recursive(tree, true, true);
            break;
        case RECURSIVE_FEATURES_ONLY:
            iterator = new Recursive(tree, true, false);
            break;
        case RECURSIVE_TREES_ONLY:
            iterator = new Recursive(tree, false, true);
            break;
        default:
            throw new IllegalArgumentException("Unrecognized strategy: " + strategy);
        }
    }

    public Strategy getStrategy() {
        return this.strategy;
    }

    @Override
    protected NodeRef computeNext() {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return endOfData();
    }

    private class Recursive extends AbstractIterator<NodeRef> {

        private boolean features;

        private boolean trees;

        private Iterator<NodeRef> myEntries;

        private Iterator<NodeRef> currEntryIterator;

        public Recursive(RevTree tree, boolean features, boolean trees) {
            Preconditions.checkArgument(features || trees);
            this.features = features;
            this.trees = trees;
            if (!features) {
                this.myEntries = new Trees(tree);
            } else {
                this.myEntries = new Children(tree);
            }
        }

        @Override
        protected NodeRef computeNext() {
            while (currEntryIterator == null || !currEntryIterator.hasNext()) {
                if (myEntries.hasNext()) {
                    currEntryIterator = resolveEntryIterator(myEntries.next());
                } else {
                    return endOfData();
                }
            }
            return currEntryIterator.next();
        }

        @SuppressWarnings("unchecked")
        private Iterator<NodeRef> resolveEntryIterator(NodeRef next) {
            if (TYPE.FEATURE.equals(next.getType())) {
                return (Iterator<NodeRef>) (features ? Iterators.singletonIterator(next)
                        : Iterators.emptyIterator());
            }
            Preconditions.checkArgument(TYPE.TREE.equals(next.getType()));
            ObjectId treeId = next.getObjectId();
            RevTree childTree = source.get(treeId, serialFactory.createRevTreeReader());
            Iterator<NodeRef> children = new Recursive(childTree, features, trees);
            if (trees) {
                children = Iterators.concat(Iterators.singletonIterator(next), children);
            }
            return children;
        }
    }

    private class Children extends AbstractIterator<NodeRef> {

        private Iterator<NodeRef> children;

        public Children(RevTree tree) {
            if (tree.buckets().isPresent()) {
                this.children = new Buckets(tree);
            } else {
                this.children = tree.children();
            }
        }

        @Override
        protected NodeRef computeNext() {
            if (children.hasNext()) {
                return children.next();
            }
            return endOfData();
        }
    }

    private class Features extends AbstractIterator<NodeRef> {

        private Iterator<NodeRef> features;

        public Features(RevTree tree) {
            if (tree.features().isPresent()) {
                this.features = tree.features().get().iterator();
            } else if (tree.buckets().isPresent()) {
                this.features = new FeatureBuckets(tree);
            } else {
                this.features = Iterators.emptyIterator();
            }
        }

        @Override
        protected NodeRef computeNext() {
            if (features.hasNext()) {
                return features.next();
            }
            return endOfData();
        }
    }

    private class Trees extends AbstractIterator<NodeRef> {

        private Iterator<NodeRef> trees;

        public Trees(RevTree tree) {
            if (tree.trees().isPresent()) {
                this.trees = tree.trees().get().iterator();
            } else if (tree.buckets().isPresent()) {
                this.trees = new TreeBuckets(tree);
            } else {
                this.trees = Iterators.emptyIterator();
            }
        }

        @Override
        protected NodeRef computeNext() {
            if (trees.hasNext()) {
                return trees.next();
            }
            return endOfData();
        }
    }

    /**
     * Returns all direct children of a buckets tree
     */
    private class Buckets extends AbstractIterator<NodeRef> {

        private Iterator<ObjectId> buckets;

        private Iterator<NodeRef> bucketEntries;

        public Buckets(RevTree tree) {
            Preconditions.checkArgument(tree.buckets().isPresent());
            buckets = tree.buckets().get().values().iterator();
        }

        @Override
        protected NodeRef computeNext() {
            while (bucketEntries == null || !bucketEntries.hasNext()) {
                if (buckets.hasNext()) {
                    bucketEntries = resolveBucketEntries(buckets.next());
                } else {
                    return endOfData();
                }
            }
            return bucketEntries.next();
        }

        /**
         * @param bucketId
         * @return
         */
        protected Iterator<NodeRef> resolveBucketEntries(ObjectId bucketId) {
            RevTree bucketTree = source.get(bucketId, serialFactory.createRevTreeReader());
            if (bucketTree.buckets().isPresent()) {
                return new Buckets(bucketTree);
            }
            return new Children(bucketTree);
        }
    }

    /**
     * Returns all direct children of a buckets tree of type TREE
     */
    private class TreeBuckets extends Buckets {

        public TreeBuckets(RevTree tree) {
            super(tree);
        }

        @Override
        protected Iterator<NodeRef> resolveBucketEntries(ObjectId bucketId) {
            RevTree bucketTree = source.get(bucketId, serialFactory.createRevTreeReader());
            if (bucketTree.buckets().isPresent()) {
                return new Buckets(bucketTree);
            }
            if (bucketTree.trees().isPresent()) {
                return new TreeBuckets(bucketTree);
            }
            return Iterators.emptyIterator();
        }
    }

    /**
     * Returns all direct children of a buckets tree of type FEATURE
     */
    private class FeatureBuckets extends Buckets {

        public FeatureBuckets(RevTree tree) {
            super(tree);
        }

        @Override
        protected Iterator<NodeRef> resolveBucketEntries(ObjectId bucketId) {
            RevTree bucketTree = source.get(bucketId, serialFactory.createRevTreeReader());
            if (bucketTree.buckets().isPresent()) {
                return new FeatureBuckets(bucketTree);
            }
            if (bucketTree.features().isPresent()) {
                return new Features(bucketTree);
            }
            return Iterators.emptyIterator();
        }
    }
}
