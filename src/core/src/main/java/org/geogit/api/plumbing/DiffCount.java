/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffCounter;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffObjectCount;
import org.geogit.api.plumbing.diff.DiffTreeWalk;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * A faster alternative to count the number of diffs between two trees than walking a
 * {@link DiffTreeWalk} iterator.
 * 
 * @see DiffCounter
 */
public class DiffCount extends AbstractGeoGitOp<DiffObjectCount> {

    private StagingDatabase index;

    private final List<String> pathFilters = Lists.newLinkedList();

    private String oldRefSpec;

    private String newRefSpec;

    private boolean reportTrees;

    @Inject
    public DiffCount(StagingDatabase index) {
        this.index = index;
    }

    public DiffCount setOldVersion(@Nullable String refSpec) {
        this.oldRefSpec = refSpec;
        return this;
    }

    public DiffCount setNewVersion(@Nullable String refSpec) {
        this.newRefSpec = refSpec;
        return this;
    }

    /**
     * @param path the path filter to use during the diff operation
     * @return {@code this}
     */
    public DiffCount addFilter(@Nullable String path) {
        if (path != null) {
            pathFilters.add(path);
        }
        return this;
    }

    /**
     * @param paths list of paths to filter by, if {@code null} or empty, then no filtering is done,
     *        otherwise the list must not contain null elements.
     */
    public DiffCount setFilter(@Nullable List<String> paths) {
        pathFilters.clear();
        if (paths != null) {
            pathFilters.addAll(paths);
        }
        return this;
    }

    @Override
    public DiffObjectCount call() {
        checkState(oldRefSpec != null, "old ref spec not provided");
        checkState(newRefSpec != null, "new ref spec not provided");

        final RevTree oldTree = getTree(oldRefSpec);
        final RevTree newTree = getTree(newRefSpec);

        DiffObjectCount diffCount;
        if (pathFilters.isEmpty()) {
            DiffCounter counter = new DiffCounter(index, oldTree, newTree);
            diffCount = counter.get();
        } else {
            DiffTreeWalk treeWalk = new DiffTreeWalk(index, oldTree, newTree);
            for (String path : pathFilters) {
                treeWalk.addFilter(path);
            }

            treeWalk.setReportTrees(reportTrees);
            Iterator<DiffEntry> iterator = treeWalk.get();
            long featureCount = 0;
            long treeCount = 0;
            while (iterator.hasNext()) {
                DiffEntry diff = iterator.next();
                TYPE type = diff.getNewObject() != null ? diff.getNewObject().getType() : diff
                        .getOldObject().getType();
                if (type.equals(TYPE.TREE)) {
                    treeCount++;
                } else {
                    featureCount++;
                }
            }
            diffCount = new DiffObjectCount(treeCount, featureCount);
        }
        return diffCount;
    }

    /**
     * @return the tree referenced by the old ref, or the head of the index.
     */
    private RevTree getTree(String refSpec) {

        Optional<ObjectId> resolved = command(ResolveTreeish.class).setTreeish(refSpec).call();
        if (!resolved.isPresent()) {
            return RevTree.EMPTY;
        }
        ObjectId headTreeId = resolved.get();
        final RevTree headTree;
        if (headTreeId.isNull()) {
            headTree = RevTree.EMPTY;
        } else {
            headTree = command(RevObjectParse.class).setObjectId(headTreeId).call(RevTree.class)
                    .get();
        }

        return headTree;
    }

    public DiffCount setReportTrees(boolean reportTrees) {
        this.reportTrees = reportTrees;
        return this;
    }

}
