/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;

import com.google.common.base.Optional;

/**
 * Deletes a branch by deleting its reference.
 * <p>
 * If trying to delete the current branch (i.e. HEAD points to that same branch), the operation
 * fails, at least {@link #setForce(boolean) force} is set to {@code true}, in which case the HEAD
 * is left in a dettached state (i.e. pointing directly to the ref's commit instead of indirectly
 * through a sym reference)
 */
public class BranchDeleteOp extends AbstractGeoGitOp<Optional<? extends Ref>> {

    private String branchName;

    private boolean force;

    public BranchDeleteOp() {
    }

    /**
     * @param branchName the name of the branch to delete, in a form {@link RefParse} understands.
     *        Must resolve to a branch reference.
     */
    public BranchDeleteOp setName(final String branchName) {
        this.branchName = branchName;
        return this;
    }

    /**
     * @param force whether to force the deletion of the branch, even if HEAD points to is (it's the
     *        current branch), has uncommitted changes, etc.
     */
    public BranchDeleteOp setForce(final boolean force) {
        this.force = force;
        return this;
    }

    /**
     * @return the reference to the branch deleted, or absent if no such branch existed
     * @throws RuntimeException if the branch couldn't be deleted
     * @throws IllegalArgumentException if the given branch name does not resolve to a branch
     *         reference (i.e. under the {@link Ref#HEADS_PREFIX heads} or
     *         {@link Ref#REMOTES_PREFIX remotes} namespace)
     */
    @Override
    public Optional<? extends Ref> call() {
        checkState(branchName != null, "Branch name not provided");
        Optional<Ref> branchRef = command(RefParse.class).setName(branchName).call();
        if (branchRef.isPresent()) {
            final Ref ref = branchRef.get();
            checkArgument(
                    ref.getName().startsWith(Ref.HEADS_PREFIX)
                            || ref.getName().startsWith(Ref.REMOTES_PREFIX), branchName
                            + " does not resolve to a branch reference: " + ref.getName());
            checkState(!(ref instanceof SymRef));

            final Optional<Ref> head = command(RefParse.class).setName(Ref.HEAD).call();

            final boolean setDettachedHead;
            if (head.isPresent() && head.get() instanceof SymRef
                    && ((SymRef) head.get()).getTarget().equals(ref.getName())) {

                checkState(force, "cannot remove the current branch at "
                        + "least force is set to true");
                setDettachedHead = true;
            } else {
                setDettachedHead = false;
            }
            UpdateRef updateRef = command(UpdateRef.class).setName(ref.getName()).setDelete(true)
                    .setReason("Delete branch " + ref.getName());
            branchRef = updateRef.call();
            checkState(branchRef.isPresent());

            if (setDettachedHead) {
                ObjectId commitId = branchRef.get().getObjectId();
                System.err.println("Updating HEAD to " + commitId + " in a dettached state");
                Optional<Ref> newHead;
                newHead = command(UpdateRef.class).setName(Ref.HEAD).setNewValue(commitId).call();
                checkState(newHead.isPresent());
                checkState(newHead.get().getObjectId().equals(commitId));
            }
        }
        return branchRef;
    }

}
