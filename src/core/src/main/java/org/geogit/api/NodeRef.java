/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

/**
 */
public class NodeRef implements Comparable<NodeRef> {

    /**
     * The character '/' used to separate paths (e.g. {@code path/to/node})
     */
    public static final char PATH_SEPARATOR = '/';

    /**
     * Full path from the root tree to the object this ref points to
     */
    private String path;

    /**
     * type of object this ref points to
     */
    private RevObject.TYPE type;

    /**
     * Id of the object this ref points to
     */
    private ObjectId objectId;

    /**
     * possibly {@link ObjectId#NULL NULL} id for the object describing the object this ref points
     * to
     */
    private ObjectId metadataId;

    public NodeRef(final String name, final ObjectId oid, final ObjectId metadataId,
            final RevObject.TYPE type) {
        checkNotNull(name);
        checkNotNull(oid);
        checkNotNull(metadataId);
        checkNotNull(type);
        this.path = name;
        this.objectId = oid;
        this.metadataId = metadataId;
        this.type = type;
    }

    /**
     * Full path from the root tree to the object this ref points to
     */
    public String getPath() {
        return path;
    }

    /**
     * The id of the object this edge points to
     */
    public ObjectId getObjectId() {
        return objectId;
    }

    /**
     * possibly {@link ObjectId#NULL NULL} id for the object describing the object this ref points
     * to
     */
    public ObjectId getMetadataId() {
        return metadataId;
    }

    /**
     * type of object this ref points to
     */
    public RevObject.TYPE getType() {
        return type;
    }

    /**
     * Tests equality over another {@code NodeRef}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeRef)) {
            return false;
        }
        NodeRef r = (NodeRef) o;
        return path.equals(r.getPath()) && type.equals(r.getType())
                && objectId.equals(r.getObjectId()) && metadataId.equals(r.getMetadataId());
    }

    /**
     * Hash code is based on name and object id
     */
    @Override
    public int hashCode() {
        return 17 ^ path.hashCode() * objectId.hashCode() * metadataId.hashCode();
    }

    /**
     * Provides for natural ordering of {@code NodeRef}, based on name
     */
    @Override
    public int compareTo(NodeRef o) {
        return path.compareTo(o.getPath());
    }

    @Override
    public String toString() {
        return new StringBuilder("NodeRef").append('[').append(path).append(" -> ")
                .append(objectId).append(']').toString();
    }

    /**
     * Returns the parent path of {@code fullPath}.
     * <p>
     * Given {@code fullPath == "path/to/node"} returns {@code "path/to"}, given {@code "node"}
     * returns {@code ""}, given {@code null} returns {@code null}
     * 
     * @param fullPath the full path to extract the parent path from
     * @return non null parent path, empty string if {@code fullPath} has no children (i.e. no
     *         {@link #PATH_SEPARATOR}).
     */
    public static @Nullable
    String parentPath(@Nullable String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return null;
        }
        int idx = fullPath.lastIndexOf(PATH_SEPARATOR);
        if (idx == -1) {
            return "";
        }
        return fullPath.substring(0, idx);
    }

    /**
     * @return true of {@code nodePath} is a direct child of {@code parentPath}, {@code false} if
     *         unrelated, sibling, same path, or nested child
     */
    public static boolean isDirectChild(String parentPath, String nodePath) {
        checkNotNull(parentPath, "parentPath");
        checkNotNull(nodePath, "nodePath");
        int idx = nodePath.lastIndexOf(PATH_SEPARATOR);
        if (parentPath.isEmpty()) {
            return !nodePath.isEmpty() && idx == -1;
        }
        return idx == parentPath.length();
    }

    /**
     * @return true of {@code nodePath} is a child of {@code parentPath} at any depth level,
     *         {@code false} if unrelated, sibling, or same path
     */
    public static boolean isChild(String parentPath, String nodePath) {
        checkNotNull(parentPath, "parentPath");
        checkNotNull(nodePath, "nodePath");
        return nodePath.length() > parentPath.length()
                && (parentPath.isEmpty() || nodePath.charAt(parentPath.length()) == PATH_SEPARATOR)
                && nodePath.startsWith(parentPath);
    }

    /**
     * Given {@code path == "path/to/node"} returns {@code ["path", "path/to", "path/to/node"]}
     * 
     * @return a sorted list of all paths that lead to the given path
     */
    public static List<String> allPathsTo(final String path) {
        checkNotNull(path);
        checkArgument(!path.isEmpty());

        StringBuilder sb = new StringBuilder();
        List<String> paths = Lists.newArrayList();

        final String[] steps = path.split("" + PATH_SEPARATOR);

        int i = 0;
        do {
            sb.append(steps[i]);
            paths.add(sb.toString());
            sb.append(PATH_SEPARATOR);
            i++;
        } while (i < steps.length);
        return paths;
    }

    /**
     * Returns a new full path made by appending {@code childName} to {@code parentTreePath}
     */
    public static String appendChild(String parentTreePath, String childName) {
        checkNotNull(parentTreePath);
        checkNotNull(childName);
        return new StringBuilder(parentTreePath).append(PATH_SEPARATOR).append(childName)
                .toString();
    }

}
