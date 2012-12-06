/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.repository.WorkingTree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Removes a feature or a tree from the working tree and the index
 * 
 */
public class RemoveOp extends AbstractGeoGitOp<WorkingTree> {

    private WorkingTree workTree;

    private List<String> pathsToRemove;

    @Inject
    public RemoveOp(final WorkingTree workTree) {
        this.workTree = workTree;
        this.pathsToRemove = new ArrayList<String>();
    }

    /**
     * @param path a path to remove
     * @return {@code this}
     */
    public RemoveOp addPathToRemove(final String path) {
        pathsToRemove.add(path);
        return this;
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    public WorkingTree call() {

        // Check that all paths are valid and exist
        for (String pathToRemove : pathsToRemove) {
            NodeRef.checkValidPath(pathToRemove);
            Optional<NodeRef> node;
            try {
                node = command(FindTreeChild.class).setParent(workTree.getTree())
                        .setChildPath(pathToRemove).call();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format(
                        "pathspec '%s' did not match any feature or tree", pathToRemove));
            }
            Preconditions.checkArgument(node.isPresent(),
                    "pathspec '%s' did not match any feature or tree", pathToRemove);
        }

        // separate trees from features an delete accordingly
        for (String pathToRemove : pathsToRemove) {
            Optional<NodeRef> node = command(FindTreeChild.class).setParent(workTree.getTree())
                    .setChildPath(pathToRemove).call();
            switch (node.get().getType()) {
            case TREE:
                workTree.delete(pathToRemove);
                break;
            case FEATURE:
                String parentPath = NodeRef.parentPath(pathToRemove);
                String name = node.get().name();
                workTree.delete(parentPath, name);
                break;
            default:
                break;
            }
        }

        return workTree;
    }

}