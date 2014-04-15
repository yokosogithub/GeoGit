/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Checks for conflicts between changes introduced by different histories, or features that have to
 * be merged.
 * 
 * This operation analyzes a merge scenario and returns true if there are conflicts or some features
 * have to be merged. This last case happens when a feature has been edited by more than one branch,
 * and the changes introduced are not the same in all of them. This usually implies creating a
 * feature not already contained in the repo, but not necessarily.
 * 
 * This return value indicates an scenario where the merge operation has to be handled differently.
 * 
 * It returns false in case there are no such issues, and the branches to be merged are completely
 * independent in their edits.
 */
public class CheckMergeScenarioOp extends AbstractGeoGitOp<Boolean> {

    private List<RevCommit> commits;

    @Inject
    public CheckMergeScenarioOp() {
    }

    /**
     * @param commits the commits to check {@link RevCommit}
     */
    public CheckMergeScenarioOp setCommits(List<RevCommit> commits) {
        this.commits = commits;
        return this;
    }

    @Override
    public Boolean call() {
        if (commits.size() < 2) {
            return Boolean.FALSE;
        }
        Optional<RevCommit> ancestor = command(FindCommonAncestor.class).setLeft(commits.get(0))
                .setRight(commits.get(1)).call();
        Preconditions.checkState(ancestor.isPresent(), "No ancestor commit could be found.");
        for (int i = 2; i < commits.size(); i++) {
            ancestor = command(FindCommonAncestor.class).setLeft(commits.get(i))
                    .setRight(ancestor.get()).call();
            Preconditions.checkState(ancestor.isPresent(), "No ancestor commit could be found.");
        }

        Map<String, List<DiffEntry>> diffs = Maps.newHashMap();
        Set<String> removedPaths = Sets.newTreeSet();

        // we organize the changes made for each path
        for (RevCommit commit : commits) {
            Iterator<DiffEntry> toMergeDiffs = command(DiffTree.class).setReportTrees(true)
                    .setOldTree(ancestor.get().getId()).setNewTree(commit.getId()).call();
            while (toMergeDiffs.hasNext()) {
                DiffEntry diff = toMergeDiffs.next();
                String path = diff.oldPath() == null ? diff.newPath() : diff.oldPath();
                if (diffs.containsKey(path)) {
                    diffs.get(path).add(diff);
                } else {
                    diffs.put(path, Lists.newArrayList(diff));
                }
                if (ChangeType.REMOVED.equals(diff.changeType())) {
                    removedPaths.add(path);
                }
            }
        }

        // now we check that, for any path, changes are compatible
        Collection<List<DiffEntry>> values = diffs.values();
        for (List<DiffEntry> list : values) {
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    if (hasConflicts(list.get(i), list.get(j))) {
                        return true;
                    }
                }
                if (!ChangeType.REMOVED.equals(list.get(i).changeType())) {
                    if (removedPaths.contains(list.get(i).getNewObject().getParentPath())) {
                        return true;
                    }
                }
            }
        }

        return false;

    }

    private boolean hasConflicts(DiffEntry diff, DiffEntry diff2) {
        if (!diff.changeType().equals(diff2.changeType())) {
            return true;
        }
        switch (diff.changeType()) {
        case ADDED:
            TYPE type = command(ResolveObjectType.class)
                    .setObjectId(diff.getNewObject().objectId()).call();
            if (TYPE.TREE.equals(type)) {
                return !diff.getNewObject().getMetadataId()
                        .equals(diff2.getNewObject().getMetadataId());
            }
            return !diff.getNewObject().objectId().equals(diff2.getNewObject().objectId());
        case REMOVED:
            break;
        case MODIFIED:
            type = command(ResolveObjectType.class).setObjectId(diff.getNewObject().objectId())
                    .call();
            if (TYPE.TREE.equals(type)) {
                return !diff.getNewObject().getMetadataId()
                        .equals(diff2.getNewObject().getMetadataId());
            } else {
                return !diff.newObjectId().equals(diff2.newObjectId());

            }
        }
        return false;
    }
}
