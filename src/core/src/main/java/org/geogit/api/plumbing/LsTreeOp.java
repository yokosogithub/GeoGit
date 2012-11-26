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
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;

/**
 * List index contents
 * 
 * @author volaya
 * 
 */
public class LsTreeOp extends AbstractGeoGitOp<Iterator<NodeRef>> {

    private WorkingTree workTree;

    private StagingDatabase index;

    private ObjectSerialisingFactory serialFactory;

    private boolean recursive;

    private String ref;

    private boolean includeTrees;

    private boolean onlyTrees;

    @Inject
    public LsTreeOp(WorkingTree workTree, StagingDatabase index,
            ObjectSerialisingFactory serialFactory) {
        this.workTree = workTree;
        this.index = index;
        this.serialFactory = serialFactory;
    }

    /**
     * @param path a path to list its content
     * @return {@code this}
     */
    public LsTreeOp setReference(final String ref) {
        this.ref = ref;
        return this;
    }

    /**
     * 
     * @param recursive if true, content of subtrees is listed recursively. If false, only content
     *        of the given path is listed
     * @return {@code this}
     */
    public LsTreeOp setRecursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    /**
     * 
     * @param showTrees if true, returns trees.
     * @return {@code this}
     */
    public LsTreeOp setIncludeTrees(boolean includeTrees) {
        this.includeTrees = includeTrees;
        return this;
    }

    /**
     * 
     * @param onlyTrees if true, returns only trees, not children.
     * @return {@code this}
     */
    public LsTreeOp setOnlyTrees(boolean onlyTrees) {
        this.onlyTrees = onlyTrees;
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

        if (!revObject.isPresent()) { // let's try to see if it is a feature type or feature in the
                                      // working tree
            NodeRef.checkValidPath(ref);
            Optional<NodeRef> treeRef = command(FindTreeChild.class).setParent(workTree.getTree())
                    .setChildPath(ref).call();
            Preconditions.checkArgument(treeRef.isPresent(), "Invalid reference: %s", ref);
            ObjectId treeId = treeRef.get().getObjectId();
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
            DepthTreeIterator iter = new DepthTreeIterator((RevTree) revObject.get(), index,
                    serialFactory);
            iter.setTraverseSubtrees(recursive);
            iter.setOnlyTrees(onlyTrees);
            iter.setIncludeTrees(includeTrees);
            return iter;
        default:
            throw new IllegalArgumentException(String.format("Invalid reference: %s", ref));
        }

    }

}