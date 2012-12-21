/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.api.plumbing.WriteTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

/**
 * 
 * Apply the changes introduced by some existing commits.
 * <p>
 * <b>NOTE:</b> so far we don't have the ability to merge non conflicting changes. Instead, the diff
 * list we get acts on whole objects, so its possible that this operation overrites non conflicting
 * changes when cherry-picking a commit that has non conflicting changes at both sides. This needs
 * to be revisited once we get more merge tools.
 * 
 */
public class CherryPickOp extends AbstractGeoGitOp<Boolean> {

    private List<ObjectId> commits;

    private Repository repository;

    private StagingArea index;

    private WorkingTree workTree;

    private Platform platform;

    /**
     * Constructs a new {@code CherryPickOp}.
     */
    @Inject
    public CherryPickOp(Repository repository, StagingArea index, WorkingTree workTree,
            Platform platform) {
        this.repository = repository;
        this.index = index;
        this.workTree = workTree;
        this.platform = platform;
    }

    /**
     * Sets the commit to replay commits onto.
     * 
     * @param onto a supplier for the commit id
     * @return {@code this}
     */
    public CherryPickOp addCommit(final Supplier<ObjectId> commit) {
        Preconditions.checkNotNull(commit);

        if (this.commits == null) {
            this.commits = new ArrayList<ObjectId>();
        }
        this.commits.add(commit.get());
        return this;
    }

    /**
     * Executes the cherry pick operation.
     * 
     * @return always {@code true}
     */
    @Override
    public Boolean call() {
        final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions
                .checkState(currHead.isPresent(), "Repository has no HEAD, can't cherry pick.");
        Preconditions.checkState(currHead.get() instanceof SymRef,
                "Can't cherry pick from detached HEAD");
        final SymRef headRef = (SymRef) currHead.get();
        final String currentBranch = headRef.getTarget();

        // count staged and unstaged changes
        long staged = index.countStaged(null);
        long unstaged = workTree.countUnstaged(null);
        Preconditions.checkState((staged == 0 && unstaged == 0),
                "You must have a clean working tree and index to perform a cherry pick.");

        getProgressListener().started();
        List<RevCommit> commitsToApply = new ArrayList<RevCommit>();

        for (ObjectId commitId : commits) {
            Preconditions.checkArgument(repository.commitExists(commitId),
                    "Commit could not be resolved: %s.", commitId);
            commitsToApply.add(repository.getCommit(commitId));
        }

        ObjectId cherryPickHead = headRef.getObjectId();

        int commitsApplied = 0;
        for (RevCommit commit : commitsToApply) {
            ObjectId parentCommitId = ObjectId.NULL;
            if (commit.getParentIds().size() > 0) {
                parentCommitId = commit.getParentIds().get(0);
            }
            ObjectId parentTreeId = ObjectId.NULL;
            if (repository.commitExists(parentCommitId)) {
                parentTreeId = repository.getCommit(parentCommitId).getTreeId();
            }
            // get changes
            Iterator<DiffEntry> diff = command(DiffTree.class).setOldTree(parentTreeId)
                    .setNewTree(commit.getTreeId()).call();
            // stage changes
            index.stage(getProgressListener(), diff, 0);
            // write new tree
            ObjectId newTreeId = command(WriteTree.class).call();
            long timestamp = platform.currentTimeMillis();
            // Create new commit
            CommitBuilder builder = new CommitBuilder(commit);
            builder.setParentIds(Arrays.asList(cherryPickHead));
            builder.setTreeId(newTreeId);
            builder.setTimestamp(timestamp);
            // builder.setCommitterTimestamp(timestamp);
            // builder.setCommitterTimeZoneOffset(TimeZone.getDefault().getOffset(timestamp));

            RevCommit newCommit = builder.build();
            repository.getObjectDatabase().put(newCommit);

            cherryPickHead = newCommit.getId();

            command(UpdateRef.class).setName(currentBranch).setNewValue(cherryPickHead).call();
            command(UpdateSymRef.class).setName(Ref.HEAD).setNewValue(currentBranch).call();

            repository.getWorkingTree().updateWorkHead(newTreeId);
            repository.getIndex().updateStageHead(newTreeId);

            commitsApplied++;

            getProgressListener().progress(commitsApplied * 100.f / commitsToApply.size());

        }

        getProgressListener().complete();

        return true;
    }
}
