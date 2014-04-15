/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Iterator;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.WriteTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsWriteOp;
import org.geogit.api.plumbing.merge.MergeScenarioReport;
import org.geogit.api.plumbing.merge.ReportCommitConflictsOp;
import org.geogit.repository.Repository;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

/**
 * 
 * Apply the changes introduced by an existing commit.
 * <p>
 * 
 */
public class CherryPickOp extends AbstractGeoGitOp<RevCommit> {

    private ObjectId commit;

    private Repository repository;

    /**
     * Constructs a new {@code CherryPickOp}.
     */
    @Inject
    public CherryPickOp(Repository repository) {
        this.repository = repository;
    }

    /**
     * Sets the commit to replay commits onto.
     * 
     * @param onto a supplier for the commit id
     * @return {@code this}
     */
    public CherryPickOp setCommit(final Supplier<ObjectId> commit) {
        Preconditions.checkNotNull(commit);

        this.commit = commit.get();
        return this;
    }

    /**
     * Executes the cherry pick operation.
     * 
     * @return RevCommit the new commit with the changes from the cherry-picked commit
     */
    @Override
    public RevCommit call() {
        final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions
                .checkState(currHead.isPresent(), "Repository has no HEAD, can't cherry pick.");
        Preconditions.checkState(currHead.get() instanceof SymRef,
                "Can't cherry pick from detached HEAD");
        final SymRef headRef = (SymRef) currHead.get();

        Preconditions.checkState(getIndex().isClean() && getWorkTree().isClean(),
                "You must have a clean working tree and index to perform a cherry pick.");

        getProgressListener().started();

        Preconditions.checkArgument(repository.commitExists(commit),
                "Commit could not be resolved: %s.", commit);
        RevCommit commitToApply = repository.getCommit(commit);

        ObjectId headId = headRef.getObjectId();

        ObjectId parentCommitId = ObjectId.NULL;
        if (commitToApply.getParentIds().size() > 0) {
            parentCommitId = commitToApply.getParentIds().get(0);
        }
        ObjectId parentTreeId = ObjectId.NULL;
        if (repository.commitExists(parentCommitId)) {
            parentTreeId = repository.getCommit(parentCommitId).getTreeId();
        }
        // get changes
        Iterator<DiffEntry> diff = command(DiffTree.class).setOldTree(parentTreeId)
                .setNewTree(commitToApply.getTreeId()).setReportTrees(true).call();

        // see if there are conflicts
        MergeScenarioReport report = command(ReportCommitConflictsOp.class)
                .setCommit(commitToApply).call();
        if (report.getConflicts().isEmpty()) {
            // stage changes
            getIndex().stage(getProgressListener(), diff, 0);
            // write new tree
            ObjectId newTreeId = command(WriteTree.class).call();
            RevCommit newCommit = command(CommitOp.class).setCommit(commitToApply).call();

            repository.getWorkingTree().updateWorkHead(newTreeId);
            repository.getIndex().updateStageHead(newTreeId);

            getProgressListener().complete();

            return newCommit;
        } else {
            Iterator<DiffEntry> unconflicted = report.getUnconflicted().iterator();
            // stage changes
            getIndex().stage(getProgressListener(), unconflicted, 0);
            getWorkTree().updateWorkHead(getIndex().getTree().getId());

            command(UpdateRef.class).setName(Ref.CHERRY_PICK_HEAD).setNewValue(commit).call();
            command(UpdateRef.class).setName(Ref.ORIG_HEAD).setNewValue(headId).call();
            command(ConflictsWriteOp.class).setConflicts(report.getConflicts()).call();

            StringBuilder msg = new StringBuilder();
            msg.append("error: could not apply ");
            msg.append(commitToApply.getId().toString().substring(0, 7));
            msg.append(" " + commitToApply.getMessage());
            for (Conflict conflict : report.getConflicts()) {
                msg.append("\t" + conflict.getPath() + "\n");
            }

            StringBuilder sb = new StringBuilder();
            for (Conflict conflict : report.getConflicts()) {
                sb.append("CONFLICT: conflict in " + conflict.getPath() + "\n");
            }
            sb.append("Fix conflicts and then commit the result using 'geogit commit -c "
                    + commitToApply.getId().toString().substring(0, 7) + "\n");
            throw new IllegalStateException(sb.toString());
        }
    }
}
