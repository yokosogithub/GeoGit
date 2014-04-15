/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.di.CanRunDuringConflict;
import org.geogit.repository.Repository;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;

/**
 * 
 * Reset current HEAD to the specified state.
 * 
 */
@CanRunDuringConflict
public class ResetOp extends AbstractGeoGitOp<Boolean> {

    /**
     * Enumeration of the possible reset modes.
     */
    public enum ResetMode {
        SOFT, MIXED, HARD, MERGE, KEEP, NONE
    };

    private Supplier<ObjectId> commit;

    private Repository repository;

    private ResetMode mode = ResetMode.NONE;

    private Set<String> patterns = new HashSet<String>();

    /**
     * Constructs a new {@code ResetOp} using the specified parameters.
     * 
     * @param repository the repository to use
     */
    @Inject
    public ResetOp(Repository repository) {
        this.repository = repository;
    }

    /**
     * Sets the reset mode.
     * 
     * @param mode the reset mode
     * @return {@code this}
     */
    public ResetOp setMode(ResetMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Sets the base commit.
     * 
     * @param commit a supplier for the {@link ObjectId id} of the commit
     * @return {@code this}
     */
    public ResetOp setCommit(final Supplier<ObjectId> commit) {
        this.commit = commit;
        return this;
    }

    /**
     * Adds a pattern.
     * 
     * @param pattern a regular expression to match what content to be reset
     * @return {@code this}
     */
    public ResetOp addPattern(final String pattern) {
        patterns.add(pattern);
        return this;
    }

    /**
     * Executes the reset operation.
     * 
     * @return always {@code true}
     */
    @Override
    public Boolean call() {
        Preconditions.checkState(!(patterns.size() > 0 && mode != ResetMode.NONE),
                "Ambiguous call, cannot specify paths and reset mode.");

        final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions.checkState(currHead.isPresent(), "Repository has no HEAD, can't reset.");
        Preconditions
                .checkState(currHead.get() instanceof SymRef, "Can't reset from detached HEAD");
        final SymRef headRef = (SymRef) currHead.get();

        final String currentBranch = headRef.getTarget();

        if (commit == null) {
            commit = Suppliers.ofInstance(currHead.get().getObjectId());
        }

        Preconditions.checkState(!ObjectId.NULL.equals(commit.get()),
                "Commit could not be resolved.");

        RevCommit oldCommit = repository.getCommit(commit.get());

        if (patterns.size() > 0) {
            for (String pattern : patterns) {
                DiffTree diffOp = command(DiffTree.class)
                        .setOldTree(repository.getIndex().getTree().getId())
                        .setNewTree(oldCommit.getTreeId()).setFilterPath(pattern);

                Iterator<DiffEntry> diff = diffOp.call();

                final long numChanges = Iterators.size(diffOp.call());
                if (numChanges == 0) {
                    // We are reseting to the current version, so there is nothing to do. However,
                    // if we are in a conflict state, the conflict should be removed and calling
                    // stage() will not do it, so we do it here
                    repository.getIndex().getDatabase().removeConflict(null, pattern);
                } else {
                    repository.getIndex().stage(subProgress((1.f / patterns.size()) * 100.f), diff,
                            numChanges);
                }
            }
        } else {
            if (mode == ResetMode.NONE) {
                mode = ResetMode.MIXED;
            }
            switch (mode) {
            case HARD:
                // Update the index and the working tree to the target tree
                getIndex().updateStageHead(oldCommit.getTreeId());
                getWorkTree().updateWorkHead(oldCommit.getTreeId());
                break;
            case SOFT:
                // Do not update index or working tree to the target tree
                break;
            case MIXED:
                // Only update the index to the target tree
                getIndex().updateStageHead(oldCommit.getTreeId());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported reset mode.");
            }

            // Update branch head to the specified commit
            command(UpdateRef.class).setName(currentBranch).setNewValue(oldCommit.getId()).call();
            command(UpdateSymRef.class).setName(Ref.HEAD).setNewValue(currentBranch).call();

            Optional<Ref> ref = command(RefParse.class).setName(Ref.MERGE_HEAD).call();
            if (ref.isPresent()) {
                command(UpdateRef.class).setName(Ref.MERGE_HEAD).setDelete(true).call();
            }
        }
        return true;
    }
}
