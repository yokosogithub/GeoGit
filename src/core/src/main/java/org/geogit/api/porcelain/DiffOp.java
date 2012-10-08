/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.diff.DiffEntry;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Perform a diff between trees pointed out by two commits
 * 
 */
public class DiffOp extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private String oldRevObjectSpec;

    private String newRevObjectSpec;

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
        this.oldRevObjectSpec = revObjectSpec;
        return this;
    }

    public DiffOp setOldVersion(ObjectId treeishOid) {
        return setOldVersion(treeishOid.toString());
    }

    public DiffOp setOldVersion(Optional<String> oldVersion) {

        return this;
    }

    /**
     * @param commitId the newVersion to set
     * @return
     */
    public DiffOp setNewVersion(String revObjectSpec) {
        this.newRevObjectSpec = revObjectSpec;
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

        if (cached) {
            // compare the tree-ish (default to HEAD) and the index
            DiffIndex diffIndex = command(DiffIndex.class);
            diffIndex.setFilter(this.pathFilter);
            if (oldRevObjectSpec != null) {
                ObjectId treeId = command(ResolveTreeish.class).setTreeish(oldRevObjectSpec).call();
                diffIndex.setRootTree(treeId);
            }
            return diffIndex.call();
        }

        if (newRevObjectSpec == null && oldRevObjectSpec == null) {
            DiffWorkTree workTreeIndexDiff = command(DiffWorkTree.class).setFilter(pathFilter);
            return workTreeIndexDiff.call();
        }

        final String oldSpec;
        final String newSpec;
        if (oldRevObjectSpec == null) {
            throw new IllegalArgumentException("old commit not specified");
        } else {
            oldSpec = oldRevObjectSpec;
        }

        if (newRevObjectSpec == null) {
            newSpec = Ref.HEAD;
        } else {
            newSpec = newRevObjectSpec;
        }

        final ObjectId oldTreeId = command(ResolveTreeish.class).setTreeish(oldSpec).call();
        final ObjectId newTreeId = command(ResolveTreeish.class).setTreeish(newSpec).call();

        Iterator<DiffEntry> iterator = command(DiffTree.class).setOldTree(oldTreeId)
                .setNewTree(newTreeId).setFilterPath(pathFilter).call();

        return iterator;
    }

}
