/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geogit.api.NodeRef.PATH_SEPARATOR;

import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.storage.NodeRefPathStorageOrder;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

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

    private NodeRefPathStorageOrder refOrder = new NodeRefPathStorageOrder();

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
        RevTree tree = objectDb.get(rootTreeId, serialFactory.createRevTreeReader());
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

        checkArgument(parentPath.isEmpty() || childPath.startsWith(parentPath + PATH_SEPARATOR));

        final List<String> allPaths = NodeRef.allPathsTo(childPath);
        final int nexChildIndex = allPaths.indexOf(parentPath) + 1;
        final String directChildPath = allPaths.get(nexChildIndex);

        final Optional<NodeRef> childTreeRef = getDirectChild(parent, directChildPath, 0);

        if (!childTreeRef.isPresent()) {
            return childTreeRef;
        }
        if (directChildPath.equals(childPath)) {
            // found it!
            return childTreeRef;
        }
        final RevTree childTree = objectDb.get(childTreeRef.get().getObjectId(),
                serialFactory.createRevTreeReader());
        return find(childTree, directChildPath, childPath);
    }

    /**
     * @param parent
     * @param directChildPath
     * @return
     */
    public Optional<NodeRef> getDirectChild(RevTree parent, String directChildPath,
            final int subtreesDepth) {
        if (parent.isEmpty()) {
            return Optional.absent();
        }

        if (parent.trees().isPresent() || parent.features().isPresent()) {
            if (parent.trees().isPresent()) {
                ImmutableList<NodeRef> refs = parent.trees().get();
                for (int i = 0; i < refs.size(); i++) {
                    if (directChildPath.equals(refs.get(i).getPath())) {
                        return Optional.of(refs.get(i));
                    }
                }
            }
            if (parent.features().isPresent()) {
                ImmutableList<NodeRef> refs = parent.features().get();
                for (int i = 0; i < refs.size(); i++) {
                    if (directChildPath.equals(refs.get(i).getPath())) {
                        return Optional.of(refs.get(i));
                    }
                }
            }
            return Optional.absent();
        }

        Integer bucket = refOrder.bucket(directChildPath, subtreesDepth);
        ImmutableSortedMap<Integer, ObjectId> buckets = parent.buckets().get();
        ObjectId subtreeId = buckets.get(bucket);
        if (subtreeId == null) {
            return Optional.absent();
        }
        RevTree subtree = objectDb.get(subtreeId, serialFactory.createRevTreeReader());
        return getDirectChild(subtree, directChildPath, subtreesDepth + 1);
    }
}
