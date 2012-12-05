/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geogit.api.NodeRef.PATH_SEPARATOR;

import java.util.List;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.storage.NodePathStorageOrder;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Searches for a {@link Node} within a particular tree.
 * 
 * @see Node
 * @see RevTree
 * @see ObjectDatabase
 */
public class DepthSearch {

    private final ObjectDatabase objectDb;

    private NodePathStorageOrder refOrder = new NodePathStorageOrder();

    /**
     * Constructs a new {@code DepthSearch} with the given parameters.
     * 
     * @param db the object database where {@link Node}s and {@link RevTree}s are stored
     */
    public DepthSearch(final ObjectDatabase db) {
        this.objectDb = db;
    }

    /**
     * Searches for a {@link Node} in the given tree.
     * 
     * @param rootTreeId the tree to search
     * @param path the path to the {@code Node} to search for
     * @return an {@link Optional} of the {@code Node} if it was found, or {@link Optional#absent()}
     *         if it wasn't found.
     */
    public Optional<NodeRef> find(final ObjectId rootTreeId, final String path) {
        RevTree tree = objectDb.get(rootTreeId, RevTree.class);
        if (tree == null) {
            return null;
        }
        return find(tree, path);
    }

    /**
     * Searches for a {@link Node} in the given tree.
     * 
     * @param rootTree the tree to search
     * @param childPath the path to the {@code Node} to search for
     * @return an {@link Optional} of the {@code Node} if it was found, or {@link Optional#absent()}
     *         if it wasn't found.
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
     * @return an {@link Optional} of the {@code Node} if the child path was found, or
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

        checkArgument(parentPath.isEmpty() || childPath.startsWith(parentPath + PATH_SEPARATOR));

        final List<String> allPaths = NodeRef.allPathsTo(childPath);
        final int nexChildIndex = allPaths.indexOf(parentPath) + 1;
        final String directChildPath = allPaths.get(nexChildIndex);
        String directChildName = NodeRef.nodeFromPath(directChildPath);

        final Optional<Node> childTreeRef = getDirectChild(parent, directChildName, 0);

        if (!childTreeRef.isPresent()) {
            return Optional.absent();
        }
        if (directChildPath.equals(childPath)) {
            // found it!
            return Optional.of(new NodeRef(childTreeRef.get(), NodeRef.parentPath(directChildPath),
                    ObjectId.NULL));
        }
        final RevTree childTree = objectDb.get(childTreeRef.get().getObjectId(), RevTree.class);
        return find(childTree, directChildPath, childPath);
    }

    /**
     * @param parent
     * @param directChildName
     * @return
     */
    public Optional<Node> getDirectChild(RevTree parent, String directChildName,
            final int subtreesDepth) {
        if (parent.isEmpty()) {
            return Optional.absent();
        }

        if (parent.trees().isPresent() || parent.features().isPresent()) {
            if (parent.trees().isPresent()) {
                ImmutableList<Node> refs = parent.trees().get();
                for (int i = 0; i < refs.size(); i++) {
                    if (directChildName.equals(refs.get(i).getName())) {
                        return Optional.of(refs.get(i));
                    }
                }
            }
            if (parent.features().isPresent()) {
                ImmutableList<Node> refs = parent.features().get();
                for (int i = 0; i < refs.size(); i++) {
                    if (directChildName.equals(refs.get(i).getName())) {
                        return Optional.of(refs.get(i));
                    }
                }
            }
            return Optional.absent();
        }

        Integer bucket = refOrder.bucket(directChildName, subtreesDepth);
        ImmutableSortedMap<Integer, ObjectId> buckets = parent.buckets().get();
        ObjectId subtreeId = buckets.get(bucket);
        if (subtreeId == null) {
            return Optional.absent();
        }
        RevTree subtree = objectDb.get(subtreeId, RevTree.class);
        return getDirectChild(subtree, directChildName, subtreesDepth + 1);
    }
}
