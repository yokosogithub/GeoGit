/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geogit.api.NodeRef.PATH_SEPARATOR;

import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Optional;

/**
 * Searches for a {@link NodeRef} within a particular tree.
 * 
 * @see NodeRef
 * @see RevTree
 * @see ObjectDatabase
 */
public class DepthSearch {

    private final ObjectDatabase objectDb;

    private ObjectSerialisingFactory serialFactory;

    /**
     * Constructs a new {@code DepthSearch} with the given parameters.
     * 
     * @param db the object database where {@link NodeRef}s and {@link RevTree}s are stored
     * @param serialFactory the serialization factor
     */
    public DepthSearch(final ObjectDatabase db, ObjectSerialisingFactory serialFactory) {
        this.objectDb = db;
        this.serialFactory = serialFactory;
    }

    /**
     * Searches for a {@link NodeRef} in the given tree.
     * 
     * @param rootTreeId the tree to search
     * @param path the path to the {@code NodeRef} to search for
     * @return an {@link Optional} of the {@code NodeRef} if it was found, or
     *         {@link Optional#absent()} if it wasn't found.
     */
    public Optional<NodeRef> find(final ObjectId rootTreeId, final String path) {
        RevTree tree = objectDb.get(rootTreeId, serialFactory.createRevTreeReader(objectDb));
        if (tree == null) {
            return null;
        }
        return find(tree, path);
    }

    /**
     * Searches for a {@link NodeRef} in the given tree.
     * 
     * @param rootTree the tree to search
     * @param childPath the path to the {@code NodeRef} to search for
     * @return an {@link Optional} of the {@code NodeRef} if it was found, or
     *         {@link Optional#absent()} if it wasn't found.
     */
    public Optional<NodeRef> find(final RevTree rootTree, final String childPath) {
        return find(rootTree, "", childPath);
    }

    /**
     * Searches for the direct child path in the parent tree.
     * 
     * @param parent the tree to search
     * @param parentPath the path of the parent tree
     * @param childPath the path to search for
     * @return an {@link Optional} of the {@code NodeRef} if the child path was found, or
     *         {@link Optional#absent()} if it wasn't found.
     */
    public Optional<NodeRef> find(final RevTree parent, final String parentPath,
            final String childPath) {
        checkNotNull(parent, "parent");
        checkNotNull(parentPath, "parentPath");
        checkNotNull(childPath, "childPath");
        checkArgument(parentPath.isEmpty()
                || parentPath.charAt(parentPath.length() - 1) != PATH_SEPARATOR);
        checkArgument(!childPath.isEmpty(), "empty child path");
        checkArgument(childPath.charAt(childPath.length() - 1) != PATH_SEPARATOR);

        checkArgument(parentPath.isEmpty() || childPath.startsWith(parentPath + PATH_SEPARATOR),
                String.format("expected: [%s/...] got: [%s]", parentPath, childPath));

        final List<String> allPaths = NodeRef.allPathsTo(childPath);
        final int nexChildIndex = allPaths.indexOf(parentPath) + 1;
        final String directChildPath = allPaths.get(nexChildIndex);
        final Optional<NodeRef> childTreeRef = parent.get(directChildPath);
        if (!childTreeRef.isPresent()) {
            return childTreeRef;
        }
        if (directChildPath.equals(childPath)) {
            // found it!
            return childTreeRef;
        }
        final RevTree childTree = objectDb.get(childTreeRef.get().getObjectId(),
                serialFactory.createRevTreeReader(objectDb));
        return find(childTree, directChildPath, childPath);
    }

    /**
     * Searches for the given {@link ObjectId} in the given tree.
     * 
     * @param tree the tree to search
     * @param childId the object to search for
     * @return the object reference if found, or {@code null} if not.
     */
    public @Nullable
    NodeRef find(final RevTree tree, final ObjectId childId) {

        final NodeRef[] refHolder = new NodeRef[1];

        class IdFindedVisitor implements TreeVisitor {

            @Override
            public boolean visitEntry(NodeRef ref) {
                if (childId.equals(ref.getObjectId())) {
                    refHolder[0] = ref;
                    return false;// end walk
                }
                if (TYPE.TREE.equals(ref.getType())) {
                    objectDb.get(ref.getObjectId(), serialFactory.createRevTreeReader(objectDb))
                            .accept(this);
                    if (refHolder[0] != null) {
                        // found, stop walk
                        return false;
                    }
                }
                return true;// continue
            }

            @Override
            public boolean visitSubTree(int bucket, ObjectId treeId) {
                return true;
            }
        }

        tree.accept(new IdFindedVisitor());
        if (refHolder[0] == null) {
            return null;
        }
        return refHolder[0];
    }
}
