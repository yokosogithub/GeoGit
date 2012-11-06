/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Create a tree object from the current index and returns the new root tree id.
 * <p>
 * Creates a tree object using the current index. The id of the new tree object is returned.
 * 
 * The index must be in a fully merged state.
 * 
 * Conceptually, write-tree sync()s the current index contents into a set of tree objects on the
 * {@link ObjectDatabase}. In order to have that match what is actually in your directory right now,
 * you need to have done a {@link UpdateIndex} phase before you did the write-tree.
 * 
 * @see FindOrCreateSubtree
 * @see DeepMove
 * @see ResolveTreeish
 * @see CreateTree
 * @see RevObjectParse
 */
public class WriteTree extends AbstractGeoGitOp<ObjectId> {

    private ObjectDatabase repositoryDatabase;

    private StagingArea index;

    private Supplier<RevTree> oldRoot;

    /**
     * Creates a new {@code WriteTree} operation using the specified parameters.
     * 
     * @param repositoryDatabase the object database to use
     * @param index the staging area
     */
    @Inject
    public WriteTree(ObjectDatabase repositoryDatabase, StagingArea index) {
        this.repositoryDatabase = repositoryDatabase;
        this.index = index;
    }

    /**
     * @param oldRoot a supplier for the old root tree
     * @return {@code this}
     */
    public WriteTree setOldRoot(Supplier<RevTree> oldRoot) {
        this.oldRoot = oldRoot;
        return this;
    }

    /**
     * Executes the write tree operation.
     * 
     * @return the new root tree id
     */
    @Override
    public ObjectId call() {
        final ProgressListener progress = getProgressListener();

        final ObjectId oldRootTreeId = resolveRootTreeId();
        final RevTree oldRootTree = resolveRootTree(oldRootTreeId);

        String pathFilter = null;
        final int numChanges = index.countStaged(pathFilter);
        if (numChanges == 0) {
            return oldRootTreeId;
        }
        if (progress.isCanceled()) {
            return null;
        }

        Iterator<DiffEntry> staged = index.getStaged(pathFilter);

        Map<String, RevTreeBuilder> changedTrees = Maps.newHashMap();

        NodeRef ref;
        int i = 0;
        while (staged.hasNext()) {
            progress.progress((float) (++i * 100) / numChanges);
            if (progress.isCanceled()) {
                return null;
            }

            DiffEntry diff = staged.next();
            ref = diff.getNewObject();

            if (ref == null) {
                ref = new NodeRef(diff.getOldObject().getPath(), ObjectId.NULL, diff.getOldObject()
                        .getMetadataId(), diff.getOldObject().getType());
            }

            final String parentPath = NodeRef.parentPath(ref.getPath());

            RevTreeBuilder parentTree = parentPath == null ? null : changedTrees.get(parentPath);

            if (parentPath != null && parentTree == null) {

                Supplier<Optional<RevTree>> rootSupp = Suppliers.ofInstance(Optional
                        .of(oldRootTree));

                parentTree = command(FindOrCreateSubtree.class).setParent(rootSupp)
                        .setChildPath(parentPath).call().builder(repositoryDatabase);// repositoryDatabase
                                                                                     // for sure?
                changedTrees.put(parentPath, parentTree);
            }

            final boolean isDelete = ObjectId.NULL.equals(ref.getObjectId());
            if (isDelete) {
                parentTree.remove(ref.getPath());
            } else {
                deepMove(ref, index.getDatabase(), repositoryDatabase);
                parentTree.put(ref);
            }
        }

        if (progress.isCanceled()) {
            return null;
        }

        // now write back all changed trees
        ObjectId newTargetRootId = oldRootTreeId;
        for (Map.Entry<String, RevTreeBuilder> e : changedTrees.entrySet()) {
            String treePath = e.getKey();
            RevTreeBuilder treeBuilder = e.getValue();
            RevTree newRoot = getTree(newTargetRootId);
            RevTree tree = treeBuilder.build();
            newTargetRootId = writeBack(newRoot.builder(repositoryDatabase), tree, treePath);
        }

        progress.complete();

        return newTargetRootId;
    }

    private RevTree getTree(ObjectId treeId) {
        if (treeId.isNull()) {
            return RevTree.EMPTY;
        }
        return command(RevObjectParse.class).setObjectId(treeId).call(RevTree.class).get();
    }

    private void deepMove(NodeRef ref, StagingDatabase indexDatabase2,
            ObjectDatabase repositoryDatabase2) {

        Supplier<NodeRef> objectRef = Suppliers.ofInstance(ref);
        command(DeepMove.class).setObjectRef(objectRef).setToIndex(false).call();

    }

    /**
     * @return the resolved root tree id
     */
    private ObjectId resolveRootTreeId() {
        if (oldRoot != null) {
            RevTree rootTree = oldRoot.get();
            return rootTree.getId();
        }
        ObjectId targetTreeId = command(ResolveTreeish.class).setTreeish(Ref.HEAD).call().get();
        return targetTreeId;
    }

    /**
     * @param targetTreeId the tree to resolve
     * @return the resolved root tree
     */
    private RevTree resolveRootTree(ObjectId targetTreeId) {
        if (oldRoot != null) {
            return oldRoot.get();
        }
        if (targetTreeId.isNull()) {
            return RevTree.EMPTY;
        }
        return command(RevObjectParse.class).setObjectId(targetTreeId).call(RevTree.class).get();
    }

    private ObjectId writeBack(RevTreeBuilder root, final RevTree tree, final String pathToTree) {

        return command(WriteBack.class).setAncestor(root).setAncestorPath("").setTree(tree)
                .setChildPath(pathToTree).setToIndex(false).call();
    }

}
