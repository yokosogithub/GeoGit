/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geotools.util.SubProgressListener;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

/**
 * 
 * Merge two or more histories together.
 * 
 */
public class MergeOp extends AbstractGeoGitOp<RevCommit> {

    private List<ObjectId> commits;

    private String message = null;

    private Repository repository;

    private StagingArea index;

    /**
     * Constructs a new {@code MergeOp} using the specified parameters.
     * 
     * @param repository the repository to use
     * @param index the staging area
     */
    @Inject
    public MergeOp(Repository repository, StagingArea index) {
        this.repository = repository;
        this.index = index;
    }

    /**
     * @param message the message for the merge commit
     * @return {@code this}
     */
    public MergeOp setMessage(final String message) {
        this.message = message;
        return this;
    }

    /**
     * Adds a commit whose history should be merged.
     * 
     * @param onto a supplier for the commit id
     * @return {@code this}
     */
    public MergeOp addCommit(final Supplier<ObjectId> commit) {
        Preconditions.checkNotNull(commit);

        if (this.commits == null) {
            this.commits = new ArrayList<ObjectId>();
        }
        this.commits.add(commit.get());
        return this;
    }

    /**
     * Executes the merge operation.
     * 
     * @return always {@code true}
     */
    @Override
    public RevCommit call() {

        final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions.checkState(currHead.isPresent(), "Repository has no HEAD, can't rebase.");
        Preconditions.checkState(currHead.get() instanceof SymRef,
                "Can't rebase from detached HEAD");
        SymRef headRef = (SymRef) currHead.get();
        final String currentBranch = headRef.getTarget();

        getProgressListener().started();

        for (ObjectId commitId : commits) {
            ProgressListener subProgress = subProgress(100.f / commits.size());

            Preconditions
                    .checkState(!ObjectId.NULL.equals(commitId), "Cannot merge a NULL commit.");
            Preconditions.checkArgument(repository.commitExists(commitId), "Not a valid commit: "
                    + commitId.toString());

            subProgress.started();
            if (ObjectId.NULL.equals(headRef.getObjectId())) {
                // Fast-forward
                command(UpdateRef.class).setName(currentBranch).setNewValue(commitId).call();
                headRef = (SymRef) command(UpdateSymRef.class).setName(Ref.HEAD)
                        .setNewValue(currentBranch).call().get();

                repository.getWorkingTree().updateWorkHead(commitId);
                repository.getIndex().updateStageHead(commitId);
                subProgress.complete();
                continue;
            }

            final RevCommit headCommit = repository.getCommit(headRef.getObjectId());
            final RevCommit targetCommit = repository.getCommit(commitId);

            Optional<RevCommit> ancestorCommit = command(FindCommonAncestor.class)
                    .setLeft(headCommit).setRight(targetCommit).call();
            subProgress.progress(10.f);

            Preconditions.checkState(ancestorCommit.isPresent(),
                    "No ancestor commit could be found.");

            if (ancestorCommit.get().getId().equals(headCommit.getId())) {
                // Fast-forward
                command(UpdateRef.class).setName(currentBranch).setNewValue(commitId).call();
                headRef = (SymRef) command(UpdateSymRef.class).setName(Ref.HEAD)
                        .setNewValue(currentBranch).call().get();

                repository.getWorkingTree().updateWorkHead(commitId);
                repository.getIndex().updateStageHead(commitId);
                subProgress.complete();
                continue;
            }

            // Get all commits between the head commit and the ancestor.
            Iterator<RevCommit> commitIterator = command(LogOp.class).setUntil(commitId).call();

            List<RevCommit> commitsToMerge = new ArrayList<RevCommit>();

            RevCommit commit;
            do {
                commit = commitIterator.next();
                commitsToMerge.add(commit);
            } while (!commit.getId().equals(ancestorCommit.get().getId()));

            int numCommits = commitsToMerge.size() - 1;

            int commitCount = 0;
            for (int i = commitsToMerge.size() - 2; i >= 0; i--) {
                commitCount++;
                // get changes
                RevCommit oldCommit = commitsToMerge.get(i);
                Iterator<DiffEntry> diff = command(DiffTree.class)
                        .setOldTree(commitsToMerge.get(i + 1).getId())
                        .setNewTree(oldCommit.getId()).call();
                // stage changes
                index.stage(new SubProgressListener(subProgress, commitCount * 100.f / numCommits),
                        diff, 0);
            }
            subProgress.complete();
        }
        String commitMessage = message;
        if (commitMessage == null) {
            commitMessage = "";
            for (ObjectId commit : commits) {
                commitMessage += "Merge commit '" + commit.toString() + "'. ";
            }
        }

        RevCommit mergeCommit = command(CommitOp.class).setMessage(commitMessage)
                .addParents(commits).call();

        getProgressListener().complete();

        return mergeCommit;
    }
}
