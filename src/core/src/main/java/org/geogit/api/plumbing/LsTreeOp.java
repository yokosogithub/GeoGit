/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DepthTreeIterator;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;

/**
 * List index contents
 * 
 */
public class LsTreeOp extends AbstractGeoGitOp<Iterator<NodeRef>> {

    public enum Strategy {
        /**
         * Default ls strategy, list the all direct child entries of a tree
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
         * Recursively list the contents of a tree in depth-first order, returning the tree ref when
         * a tree node is found followed by the contents of the subtree
         */
        DEPTHFIRST,
        /**
         * Recursively list the contents of a tree in depth-first order, but do not report TREE
         * entries, only FEATURE ones
         */
        DEPTHFIRST_ONLY_FEATURES,
        /**
         * Recursively list the contents of a tree in depth-first order, but do not report TREE
         * entries, only FEATURE ones
         */
        DEPTHFIRST_ONLY_TREES
    }

    private WorkingTree workTree;

    private StagingDatabase index;

    private Strategy strategy;

    private String ref;

    @Inject
    public LsTreeOp(WorkingTree workTree, StagingDatabase index) {
        this.workTree = workTree;
        this.index = index;
        this.strategy = Strategy.CHILDREN;
    }

    /**
     * @param path a path to list its content
     * @return {@code this}
     */
    public LsTreeOp setReference(final String ref) {
        this.ref = ref;
        return this;
    }

    public LsTreeOp setStrategy(final Strategy strategy) {
        Preconditions.checkNotNull(strategy);
        this.strategy = strategy;
        return this;
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    public Iterator<NodeRef> call() {

        if (ref == null) {
            ref = Ref.WORK_HEAD;
        }

        Optional<RevObject> revObject = command(RevObjectParse.class).setRefSpec(ref).call(
                RevObject.class);

        ObjectId parentObjectId = ObjectId.NULL;

        if (!revObject.isPresent()) { // let's try to see if it is a feature type or feature in the
                                      // working tree
            NodeRef.checkValidPath(ref);
            Optional<NodeRef> treeRef = command(FindTreeChild.class).setParent(workTree.getTree())
                    .setChildPath(ref).call();
            Preconditions.checkArgument(treeRef.isPresent(), "Invalid reference: %s", ref);
            ObjectId treeId = treeRef.get().objectId();
            parentObjectId = treeRef.get().getMetadataId();
            revObject = command(RevObjectParse.class).setObjectId(treeId).call(RevObject.class);
        }

        checkArgument(revObject.isPresent(), "Invalid reference: %s", ref);

        final TYPE type = revObject.get().getType();
        switch (type) {
        case FEATURE:
            NodeRef nodeRef = null;
            return Iterators.forArray(new NodeRef[] { nodeRef });
        case COMMIT:
            RevCommit revCommit = (RevCommit) revObject.get();
            ObjectId treeId = revCommit.getTreeId();
            revObject = command(RevObjectParse.class).setObjectId(treeId).call(RevObject.class);
        case TREE:

            DepthTreeIterator.Strategy iterStrategy;

            switch (this.strategy) {
            case CHILDREN:
                iterStrategy = DepthTreeIterator.Strategy.CHILDREN;
                break;
            case FEATURES_ONLY:
                iterStrategy = DepthTreeIterator.Strategy.FEATURES_ONLY;
                break;
            case TREES_ONLY:
                iterStrategy = DepthTreeIterator.Strategy.TREES_ONLY;
                break;
            case DEPTHFIRST:
                iterStrategy = DepthTreeIterator.Strategy.RECURSIVE;
                break;
            case DEPTHFIRST_ONLY_FEATURES:
                iterStrategy = DepthTreeIterator.Strategy.RECURSIVE_FEATURES_ONLY;
                break;
            case DEPTHFIRST_ONLY_TREES:
                iterStrategy = DepthTreeIterator.Strategy.RECURSIVE_TREES_ONLY;
                break;
            default:
                throw new IllegalStateException("Unknown strategy: " + this.strategy);
            }

            final String path = ref.lastIndexOf(':') != -1 ? ref
                    .substring(ref.lastIndexOf(':') + 1) : "";

            DepthTreeIterator iter = new DepthTreeIterator(path, parentObjectId,
                    (RevTree) revObject.get(), index, iterStrategy);
            return iter;
        default:
            throw new IllegalArgumentException(String.format("Invalid reference: %s", ref));
        }

    }
}