/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectDatabase;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Create a tree object from the current index and returns the new root tree id.
 * <p>
 * Creates a tree object using the current index. The id of the new root tree object is returned.
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

        final RevTree oldRootTree = resolveRootTree();

        Iterator<DiffEntry> staged = index.getStaged(null);
        if (!staged.hasNext()) {
            return oldRootTree.getId();
        }
        if (progress.isCanceled()) {
            return null;
        }

        Map<String, RevTreeBuilder> repositoryChangedTrees = Maps.newHashMap();
        Map<String, NodeRef> indexChangedTrees = Maps.newHashMap();
        Map<String, ObjectId> changedTreesMetadataId = Maps.newHashMap();

        NodeRef ref;
        int i = 0;
        final long numChanges = index.countStaged(null);
        while (staged.hasNext()) {
            progress.progress((float) (++i * 100) / numChanges);
            if (progress.isCanceled()) {
                return null;
            }

            DiffEntry diff = staged.next();
            ref = diff.getNewObject();

            if (ref == null) {
                ObjectId metadataId = diff.getOldObject().getMetadataId();
                ref = new NodeRef(diff.getOldObject().getNode(), diff.getOldObject()
                        .getParentPath(), metadataId);
            }

            final String parentPath = ref.getParentPath();

            RevTreeBuilder parentTree = resolveTargetTree(oldRootTree, parentPath,
                    repositoryChangedTrees, changedTreesMetadataId);

            resolveSourceTreeRef(parentPath, indexChangedTrees, changedTreesMetadataId);

            Preconditions.checkState(parentTree != null);

            final boolean isDelete = ChangeType.REMOVED.equals(diff.changeType());
            if (isDelete) {
                parentTree.remove(diff.getOldObject().getNode().getName());
            } else {
                deepMove(ref.getNode());
                parentTree.put(ref.getNode());
            }
        }

        if (progress.isCanceled()) {
            return null;
        }

        // now write back all changed trees
        ObjectId newTargetRootId = oldRootTree.getId();
        for (Map.Entry<String, RevTreeBuilder> e : repositoryChangedTrees.entrySet()) {
            String treePath = e.getKey();
            ObjectId metadataId = changedTreesMetadataId.get(treePath);
            RevTreeBuilder treeBuilder = e.getValue();
            RevTree newRoot = getTree(newTargetRootId);
            RevTree tree = treeBuilder.build();
            newTargetRootId = writeBack(newRoot.builder(repositoryDatabase), tree, treePath,
                    metadataId);
        }

        progress.complete();

        return newTargetRootId;
    }

    /**
     * @param parentPath2
     * @param indexChangedTrees
     * @param metadataCache
     * @return
     */
    private void resolveSourceTreeRef(String parentPath, Map<String, NodeRef> indexChangedTrees,
            Map<String, ObjectId> metadataCache) {

        NodeRef indexTreeRef = indexChangedTrees.get(parentPath);

        if (indexTreeRef == null) {
            RevTree stageHead = index.getTree();
            Optional<NodeRef> treeRef = command(FindTreeChild.class).setIndex(true)
                    .setParent(stageHead).setChildPath(parentPath).call();
            if (treeRef.isPresent()) {// may not be in case of a delete
                indexTreeRef = treeRef.get();
                indexChangedTrees.put(parentPath, indexTreeRef);
                metadataCache.put(parentPath, indexTreeRef.getMetadataId());
            }
        } else {
            metadataCache.put(parentPath, indexTreeRef.getMetadataId());
        }
    }

    /**
     * @param treePath
     * @param treeCache
     * @param metadataCache
     * @return
     */
    private RevTreeBuilder resolveTargetTree(final RevTree root, String treePath,
            Map<String, RevTreeBuilder> treeCache, Map<String, ObjectId> metadataCache) {

        RevTreeBuilder treeBuilder = treeCache.get(treePath);
        if (treeBuilder == null) {
            Optional<NodeRef> treeRef = command(FindTreeChild.class).setIndex(false)
                    .setParent(root).setChildPath(treePath).call();
            if (treeRef.isPresent()) {
                metadataCache.put(treePath, treeRef.get().getMetadataId());
                treeBuilder = command(RevObjectParse.class).setObjectId(treeRef.get().objectId())
                        .call(RevTree.class).get().builder(repositoryDatabase);
                treeCache.put(treePath, treeBuilder);
            } else {
                metadataCache.put(treePath, ObjectId.NULL);
                treeBuilder = new RevTreeBuilder(repositoryDatabase);
                treeCache.put(treePath, treeBuilder);
            }
        }
        return treeBuilder;
    }

    private RevTree getTree(ObjectId treeId) {
        if (treeId.isNull()) {
            return RevTree.EMPTY;
        }
        return command(RevObjectParse.class).setObjectId(treeId).call(RevTree.class).get();
    }

    private void deepMove(Node ref) {

        Supplier<Node> objectRef = Suppliers.ofInstance(ref);
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
     * @return the resolved root tree
     */
    private RevTree resolveRootTree() {
        if (oldRoot != null) {
            return oldRoot.get();
        }
        final ObjectId targetTreeId = resolveRootTreeId();
        if (targetTreeId.isNull()) {
            return RevTree.EMPTY;
        }
        return command(RevObjectParse.class).setObjectId(targetTreeId).call(RevTree.class).get();
    }

    private ObjectId writeBack(RevTreeBuilder root, final RevTree tree, final String pathToTree,
            final ObjectId metadataId) {

        return command(WriteBack.class).setAncestor(root).setAncestorPath("").setTree(tree)
                .setChildPath(pathToTree).setToIndex(false).setMetadataId(metadataId).call();
    }

}
