/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;

/**
 * Writes the contents of a given tree as a child of a given ancestor tree, creating any
 * intermediate tree needed, and returns the {@link ObjectId id} of the resulting new ancestor tree.
 * <p>
 * If no {@link #setAncestor(MutableTree) ancestor} is provided,it is assumed to be the current HEAD
 * tree.
 * <p>
 * If no {@link #setAncestorPath(String) ancestor path} is provided, the ancestor is assumed to be a
 * root tree
 * 
 * @see CreateTree
 * @see RevObjectParse
 * @see FindTreeChild
 */
public class WriteBack extends AbstractGeoGitOp<ObjectId> {

    private final ObjectSerialisingFactory serialFactory;

    private final ObjectDatabase odb;

    private final StagingDatabase index;

    private boolean indexDb;

    private Supplier<MutableTree> ancestor;

    private String childPath;

    private Supplier<RevTree> tree;

    private String ancestorPath;

    /**
     * Constructs a new {@code WriteBack} operation with the given parameters.
     * 
     * @param odb the object database to use
     * @param index the staging database to use
     * @param serialFactory the serialization factory
     */
    @Inject
    public WriteBack(ObjectDatabase odb, StagingDatabase index,
            ObjectSerialisingFactory serialFactory) {
        this.odb = odb;
        this.index = index;
        this.serialFactory = serialFactory;
    }

    /**
     * @param indexDb if {@code true} the trees will be stored to the {@link StagingDatabase},
     *        otherwise to the repository's {@link ObjectDatabase permanent store}. Defaults to
     *        {@code false}
     * @return {@code this}
     */
    public WriteBack setIndex(boolean indexDb) {
        this.indexDb = indexDb;
        return this;
    }

    /**
     * @param oldRoot the root tree to which add the {@link #setTree(RevTree) child tree} and any
     *        intermediate tree. If not set defaults to the current HEAD tree
     * @return {@code this}
     */
    public WriteBack setAncestor(MutableTree oldRoot) {
        return setAncestor(Suppliers.ofInstance(oldRoot));
    }

    /**
     * @param ancestor the root tree to which add the {@link #setTree(RevTree) child tree} and any
     *        intermediate tree. If not set defaults to the current HEAD tree
     * @return {@code this}
     */
    public WriteBack setAncestor(Supplier<MutableTree> ancestor) {
        this.ancestor = ancestor;
        return this;
    }

    /**
     * @param ancestorPath the path of the {@link #setAncestor(Supplier) ancestor tree}. If set not
     *        the ancestor tree is assumed to be a root tree.
     * @return {@code this}
     */
    public WriteBack setAncestorPath(String ancestorPath) {
        this.ancestorPath = ancestorPath;
        return this;
    }

    /**
     * @param childPath mandatory, the path to the child tree
     * @return {@code this}
     */
    public WriteBack setChildPath(String childPath) {
        this.childPath = childPath;
        return this;
    }

    /**
     * @param tree the tree to store on the object database and to create any intermediate tree for
     *        the given {@link #setAncestor(Supplier) ancestor tree}
     * @return {@code this}
     */
    public WriteBack setTree(RevTree tree) {
        return setTree(Suppliers.ofInstance(tree));
    }

    /**
     * @param tree the tree to store on the object database and to create any intermediate tree for
     *        the given {@link #setAncestor(Supplier) ancestor tree}
     * @return {@code this}
     */
    public WriteBack setTree(Supplier<RevTree> tree) {
        this.tree = tree;
        return this;
    }

    /**
     * Executes the write back operation.
     * 
     * @return the {@link ObjectId id} of the resulting new ancestor tree.
     */
    @Override
    public ObjectId call() {
        checkNotNull(tree, "child tree not set");
        checkNotNull(childPath, "child tree path not set");

        String ancestorPath = resolveAncestorPath();
        checkArgument(NodeRef.isChild(ancestorPath, childPath), String.format(
                "child path '%s' is not a child of ancestor path '%s'", childPath, ancestorPath));

        RevTree tree = this.tree.get();
        checkState(null != tree, "child tree supplier returned null");

        ObjectDatabase targetDb = indexDb ? index : odb;
        MutableTree root = resolveAncestor();

        return writeBack(root, ancestorPath, tree, childPath, targetDb);
    }

    /**
     * @return the resolved ancestor path
     */
    private String resolveAncestorPath() {
        return ancestorPath == null ? "" : ancestorPath;
    }

    /**
     * @return the resolved ancestor
     */
    private MutableTree resolveAncestor() {
        if (this.ancestor == null) {

        }
        MutableTree ancestor = this.ancestor.get();
        checkState(ancestor != null, "provided ancestor tree supplier returned null");
        return ancestor;
    }

    private ObjectId writeBack(MutableTree ancestor, final String ancestorPath,
            final RevTree childTree, final String childPath, final ObjectDatabase targetDatabase) {

        final ObjectId treeId = command(HashObject.class).setObject(childTree).call();
        targetDatabase.put(treeId, serialFactory.createRevTreeWriter(childTree));

        final boolean isDirectChild = NodeRef.isDirectChild(ancestorPath, childPath);
        if (isDirectChild) {
            ObjectId metadataId = ObjectId.NULL;
            ancestor.put(new NodeRef(childPath, treeId, metadataId, TYPE.TREE));
            ObjectWriter<RevTree> treeWriter = serialFactory.createRevTreeWriter(ancestor);
            ObjectId newAncestorId = command(HashObject.class).setObject(ancestor).call();
            targetDatabase.put(newAncestorId, treeWriter);
            return newAncestorId;
        }

        final String parentPath = NodeRef.parentPath(childPath);
        Optional<NodeRef> parentRef = getTreeChild(ancestor, parentPath);
        MutableTree parent;
        if (parentRef.isPresent()) {
            ObjectId parentId = parentRef.get().getObjectId();
            parent = getTree(parentId).mutable();
        } else {
            parent = newTree();
        }

        parent.put(new NodeRef(childPath, treeId, ObjectId.NULL, TYPE.TREE));

        return writeBack(ancestor, ancestorPath, parent, parentPath, targetDatabase);
    }

    private MutableTree newTree() {
        return command(CreateTree.class).setIndex(indexDb).call();
    }

    private RevTree getTree(ObjectId treeId) {
        if (treeId.isNull()) {
            return newTree();
        }
        ObjectDatabase targetDb = indexDb ? index : odb;
        RevTree revTree = targetDb.get(treeId, serialFactory.createRevTreeReader(targetDb));
        return revTree;
    }

    private Optional<NodeRef> getTreeChild(RevTree parent, String childPath) {

        FindTreeChild cmd = command(FindTreeChild.class).setIndex(true).setParent(parent)
                .setChildPath(childPath);

        return cmd.call();
    }

}
