/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.DiffEntry;

import com.google.inject.Inject;

/**
 * Perform a diff between trees pointed out by two commits
 * <p>
 * Usage:
 * <ul>
 * <li>
 * <code>{@link #setOldVersion(String) oldVersion} == null && {@link #setNewVersion(String) newVersion} == null</code>
 * : compare working tree and index
 * <li>
 * <code>{@link #setOldVersion(String) oldVersion} != null && {@link #setNewVersion(String) newVersion} == null</code>
 * : compare the working tree with the given commit
 * <li>
 * <code>{@link #setCompareIndex(boolean) compareIndex} == true && {@link #setOldVersion(String) oldVersion} == null && {@link #setNewVersion(String) newVersion} == null</code>
 * : compare the index with the HEAD commit
 * <li>
 * <code>{@link #setCompareIndex(boolean) compareIndex} == true && {@link #setOldVersion(String) oldVersion} != null && {@link #setNewVersion(String) newVersion} == null</code>
 * : compare the index with the given commit
 * <li>
 * <code>{@link #setOldVersion(String) oldVersion} != null && {@link #setNewVersion(String) newVersion} != null</code>
 * : compare {@code commit1} with {@code commit2}, where {@code commit1} is the eldest or left side
 * of the diff.
 * </ul>
 * 
 * @see DiffWorkTree
 * @see DiffIndex
 * @see DiffTree
 */
public class DiffOp extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private String oldRefSpec;

    private String newRefSpec;

    private String pathFilter;

    private boolean cached;

    @Inject
    public DiffOp() {
    }

    public void setCompareIndex(boolean compareIndex) {
        this.cached = compareIndex;
    }

    /**
     * @param commitId the oldVersion to set
     * @return
     */
    public DiffOp setOldVersion(@Nullable String revObjectSpec) {
        this.oldRefSpec = revObjectSpec;
        return this;
    }

    public DiffOp setOldVersion(ObjectId treeishOid) {
        return setOldVersion(treeishOid.toString());
    }

    /**
     * @param commitId the newVersion to set
     * @return
     */
    public DiffOp setNewVersion(String revObjectSpec) {
        this.newRefSpec = revObjectSpec;
        return this;
    }

    public DiffOp setNewVersion(ObjectId treeishOid) {
        return setNewVersion(treeishOid.toString());
    }

    public DiffOp setFilter(String pathFilter) {
        this.pathFilter = pathFilter;
        return this;
    }

    @Override
    public Iterator<DiffEntry> call() {
        checkArgument(cached && oldRefSpec == null || !cached, String.format(
                "compare index allows only one revision to check against, got %s / %s", oldRefSpec,
                newRefSpec));
        checkArgument(newRefSpec == null || oldRefSpec != null,
                "If new rev spec is specified then old rev spec is mandatory");

        Iterator<DiffEntry> iterator;
        if (cached) {
            // compare the tree-ish (default to HEAD) and the index
            DiffIndex diffIndex = command(DiffIndex.class).setFilter(this.pathFilter);
            if (oldRefSpec != null) {
                diffIndex.setOldVersion(oldRefSpec);
            }
            iterator = diffIndex.call();
        } else if (newRefSpec == null) {

            DiffWorkTree workTreeIndexDiff = command(DiffWorkTree.class).setFilter(pathFilter);
            if (oldRefSpec != null) {
                workTreeIndexDiff.setOldVersion(oldRefSpec);
            }
            iterator = workTreeIndexDiff.call();
        } else {

            iterator = command(DiffTree.class).setOldVersion(oldRefSpec).setNewVersion(newRefSpec)
                    .setFilterPath(pathFilter).call();
        }

        return iterator;
    }

}
