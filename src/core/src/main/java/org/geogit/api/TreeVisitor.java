/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * Provides an interface for building classes that visit {@link NodeRef}s and sub trees of a tree.
 */
public interface TreeVisitor {

    /**
     * Visits the provided {@link NodeRef}.
     * 
     * @param ref the node to visit
     * @return false if the visitor has finished
     */
    boolean visitEntry(NodeRef ref);

    /**
     * Visits the tree that is referred to by the given id.
     * 
     * @param bucket the bucket to visit
     * @param treeId the tree to visit
     * @return false if the visitor has finished
     */
    boolean visitSubTree(int bucket, ObjectId treeId);

}
