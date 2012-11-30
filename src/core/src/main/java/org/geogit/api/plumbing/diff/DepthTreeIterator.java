/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.hessian.HessianFactory;

import com.google.common.base.Function;
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

    private Iterator<NodeRef> iterator;

    private ObjectDatabase source;

    private ObjectSerialisingFactory serialFactory = new HessianFactory();

    private Strategy strategy;

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

    // public DepthTreeIterator(RevTree tree, ObjectDatabase source, Strategy strategy) {
    // this("", ObjectId.NULL, tree, source, strategy);
    // }

    public DepthTreeIterator(final String treePath, final ObjectId metadataId, RevTree tree,
            ObjectDatabase source, Strategy strategy) {
        checkNotNull(treePath);
        checkNotNull(metadataId);
        checkNotNull(tree);
        checkNotNull(source);
        checkNotNull(strategy);

        NodeToRef functor = new NodeToRef(treePath, metadataId);
        this.source = source;
        this.strategy = strategy;
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

        private Iterator<NodeRef> resolveEntryIterator(Node next) {
            if (TYPE.FEATURE.equals(next.getType())) {
                if (features) {
                    return Iterators.singletonIterator(functor.apply(next));
                }
                return Iterators.emptyIterator();
            }
            Preconditions.checkArgument(TYPE.TREE.equals(next.getType()));

            ObjectId treeId = next.getObjectId();
            RevTree childTree = source.get(treeId, serialFactory.createRevTreeReader());

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
                this.children = tree.children();
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
                this.features = tree.features().get().iterator();
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
            if (tree.trees().isPresent()) {
                this.trees = tree.trees().get().iterator();
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

        private Iterator<ObjectId> buckets;

        private Iterator<Node> bucketEntries;

        public Buckets(RevTree tree) {
            Preconditions.checkArgument(tree.buckets().isPresent());
            buckets = tree.buckets().get().values().iterator();
        }

        @Override
        protected Node computeNext() {
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
        protected Iterator<Node> resolveBucketEntries(ObjectId bucketId) {
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
        protected Iterator<Node> resolveBucketEntries(ObjectId bucketId) {
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
        protected Iterator<Node> resolveBucketEntries(ObjectId bucketId) {
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
