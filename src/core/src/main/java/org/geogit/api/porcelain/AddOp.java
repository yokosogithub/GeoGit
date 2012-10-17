/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.HashSet;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.repository.WorkingTree;

import com.google.inject.Inject;

/**
 * Manipulates the index (staging area) by setting the unstaged changes that match this operation criteria as staged.
 * 
 * @author groldan
 * 
 */
public class AddOp extends AbstractGeoGitOp<WorkingTree> {

    private Set<String> patterns;

    private boolean updateOnly;

    private WorkingTree workTree;

    @Inject
    public AddOp(final WorkingTree workTree) {
        this.workTree = workTree;
        patterns = new HashSet<String>();
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    public WorkingTree call() {
        // this is add all, TODO: implement partial adds
        String path = null;
        if (patterns.size() == 1) {
            path = patterns.iterator().next();
        }
        workTree.stage(getProgressListener(), path);
        return workTree;
    }

    /**
     * @param pattern a regular expression to match what content to be staged
     * @return {@code this}
     */
    public AddOp addPattern(final String pattern) {
        patterns.add(pattern);
        return this;
    }

    /**
     * @param updateOnly if {@code true}, only add already tracked features (either for modification or deletion), but do not stage any newly added
     *        one.
     * @return {@code this}
     */
    public AddOp setUpdateOnly(final boolean updateOnly) {
        this.updateOnly = updateOnly;
        return this;
    }

}
