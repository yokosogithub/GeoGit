/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.Bounded;
import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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

    private Iterator<NodeRef> iterator;

    private ObjectDatabase source;

    private Strategy strategy;

    private Predicate<Bounded> boundsFilter;

    private NodeToRef functor;

    private RevTree tree;

    private String treePath;

    private ObjectId metadataId;

    private static class NodeToRef implements Function<Node, NodeRef> {

        private final String treePath;

        private final ObjectId metadataId;

        public NodeToRef(String treePath, ObjectId metadataId) {
            this.treePath = treePath;
            this.metadataId = metadataId;
        }

        @Override
        public NodeRef apply(Node node) {
            return new NodeRef(node, treePath, node.getMetadataId().or(metadataId));
        }
    };

    public DepthTreeIterator(final String treePath, final ObjectId metadataId, RevTree tree,
            ObjectDatabase source, Strategy strategy) {
        checkNotNull(treePath);
        checkNotNull(metadataId);
        checkNotNull(tree);
        checkNotNull(source);
        checkNotNull(strategy);

        this.tree = tree;
        this.treePath = treePath;
        this.metadataId = metadataId;
        this.source = source;
        this.strategy = strategy;
        this.functor = new NodeToRef(treePath, metadataId);
        this.boundsFilter = Predicates.alwaysTrue();
    }

    public void setBoundsFilter(@Nullable Predicate<Bounded> boundsFilter) {
        Predicate<Bounded> alwaysTrue = Predicates.alwaysTrue();
        this.boundsFilter = boundsFilter == null ? alwaysTrue : boundsFilter;
    }

    @Override
    protected NodeRef computeNext() {
        if (iterator == null) {
            switch (strategy) {
            case CHILDREN:
                iterator = Iterators.transform(new Children(tree), functor);
                break;
            case FEATURES_ONLY:
                iterator = Iterators.transform(new Features(tree), functor);
                break;
            case TREES_ONLY:
                iterator = Iterators.transform(new Trees(tree), functor);
                break;
            case RECURSIVE:
                iterator = new Recursive(treePath, metadataId, tree, true, true);
                break;
            case RECURSIVE_FEATURES_ONLY:
                iterator = new Recursive(treePath, metadataId, tree, true, false);
                break;
            case RECURSIVE_TREES_ONLY:
                iterator = new Recursive(treePath, metadataId, tree, false, true);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized strategy: " + strategy);
            }

        }
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return endOfData();
    }

    private class Recursive extends AbstractIterator<NodeRef> {

        private boolean features;

        private boolean trees;

        private Iterator<Node> myEntries;

        private Iterator<NodeRef> currEntryIterator;

        private NodeToRef functor;

        public Recursive(String treePath, ObjectId metadataId, RevTree tree, boolean features,
                boolean trees) {
            Preconditions.checkArgument(features || trees);
            this.functor = new NodeToRef(treePath, metadataId);
            this.features = features;
            this.trees = trees;
            if (!features) {
                this.myEntries = new Trees(tree);
            } else {
                this.myEntries = new Children(tree);
            }
            currEntryIterator = Iterators.emptyIterator();
        }

        @Override
        protected NodeRef computeNext() {
            while (!currEntryIterator.hasNext()) {
                if (myEntries.hasNext()) {
                    currEntryIterator = resolveEntryIterator(myEntries.next());
                } else {
                    return endOfData();
                }
            }
            return currEntryIterator.next();
        }

        private Iterator<NodeRef> resolveEntryIterator(Node next) {
            if (TYPE.FEATURE.equals(next.getType())) {
                if (features) {
                    return Iterators.singletonIterator(functor.apply(next));
                }
                return Iterators.emptyIterator();
            }
            Preconditions.checkArgument(TYPE.TREE.equals(next.getType()));

            ObjectId treeId = next.getObjectId();
            RevTree childTree = source.getTree(treeId);

            String childTreePath = NodeRef.appendChild(this.functor.treePath, next.getName());
            Iterator<NodeRef> children = new Recursive(childTreePath, next.getMetadataId().or(
                    functor.metadataId), childTree, features, trees);
            if (trees) {
                children = Iterators.concat(Iterators.singletonIterator(functor.apply(next)),
                        children);
            }
            return children;
        }
    }

    private class Children extends AbstractIterator<Node> {

        private Iterator<Node> children;

        public Children(RevTree tree) {
            if (tree.buckets().isPresent()) {
                this.children = new Buckets(tree);
            } else {
                this.children = Iterators.filter(tree.children(), boundsFilter);
            }
        }

        @Override
        protected Node computeNext() {
            if (children.hasNext()) {
                return children.next();
            }
            return endOfData();
        }
    }

    private class Features extends AbstractIterator<Node> {

        private Iterator<Node> features;

        public Features(RevTree tree) {
            if (tree.features().isPresent()) {
                this.features = Iterators.filter(tree.features().get().iterator(), boundsFilter);
            } else if (tree.buckets().isPresent()) {
                this.features = new FeatureBuckets(tree);
            } else {
                this.features = Iterators.emptyIterator();
            }
        }

        @Override
        protected Node computeNext() {
            if (features.hasNext()) {
                return features.next();
            }
            return endOfData();
        }
    }

    private class Trees extends AbstractIterator<Node> {

        private Iterator<Node> trees;

        public Trees(RevTree tree) {
            if (tree.numTrees() == 0) {
                this.trees = Iterators.emptyIterator();
            } else if (tree.trees().isPresent()) {
                this.trees = Iterators.filter(tree.trees().get().iterator(), boundsFilter);
            } else if (tree.buckets().isPresent()) {
                this.trees = new TreeBuckets(tree);
            } else {
                this.trees = Iterators.emptyIterator();
            }
        }

        @Override
        protected Node computeNext() {
            if (trees.hasNext()) {
                return trees.next();
            }
            return endOfData();
        }
    }

    /**
     * Returns all direct children of a buckets tree
     */
    private class Buckets extends AbstractIterator<Node> {

        private Iterator<Bucket> buckets;

        private Iterator<Node> bucketEntries;

        public Buckets(RevTree tree) {
            Preconditions.checkArgument(tree.buckets().isPresent());
            buckets = Iterators.filter(tree.buckets().get().values().iterator(), boundsFilter);
            bucketEntries = Iterators.emptyIterator();
        }

        @Override
        protected Node computeNext() {
            while (!bucketEntries.hasNext()) {
                if (buckets.hasNext()) {
                    Bucket nextBucket = buckets.next();
                    bucketEntries = resolveBucketEntries(nextBucket.id());
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
        protected Iterator<Node> resolveBucketEntries(ObjectId bucketId) {
            RevTree bucketTree = source.getTree(bucketId);
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
        protected Iterator<Node> resolveBucketEntries(ObjectId bucketId) {
            RevTree bucketTree = source.getTree(bucketId);
            if (bucketTree.numTrees() == 0) {
                return Iterators.emptyIterator();
            }
            if (bucketTree.trees().isPresent()) {
                return new Trees(bucketTree);
            }
            if (bucketTree.buckets().isPresent()) {
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
        protected Iterator<Node> resolveBucketEntries(ObjectId bucketId) {
            RevTree bucketTree = source.getTree(bucketId);
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
