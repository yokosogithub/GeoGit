/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.UpdateRef;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Creates a new head ref (branch) pointing to the specified tree-ish or the current HEAD if no
 * tree-ish was specified.
 * <p>
 * 
 * @TODO: support branch descriptions
 * @TODO: support setting up the branch to track a remote branch
 */
public class BranchCreateOp extends AbstractGeoGitOp<Ref> {

    private String branchName;

    private String commit_ish;

    private boolean checkout;

    private boolean orphan;

    private boolean force;

    @Inject
    public BranchCreateOp() {
    }

    /**
     * @param branchName the name of the branch to create, must not already exist
     */
    public BranchCreateOp setName(final String branchName) {
        this.branchName = branchName;
        return this;
    }

    /**
     * @param commit_ish either a branch ref or commit id where this branch starts at. If not set
     *        defaults to the current {@link Ref#HEAD HEAD}
     */
    public BranchCreateOp setSource(@Nullable String commit_ish) {
        this.commit_ish = commit_ish;
        return this;
    }

    /**
     * @param force true if the branch should overwrite an exisiting one with the same name, in case
     *        it exists
     */
    public BranchCreateOp setForce(boolean force) {
        this.force = force;
        return this;
    }

    /**
     * @param orphan {@code true} if the new branch shares no history with the current one, defaults
     *        to {@code false}
     */
    public BranchCreateOp setOrphan(boolean orphan) {
        this.orphan = orphan;
        return this;
    }

    /**
     * @param checkout if {@code true}, in addition to creating the new branch, a {@link CheckoutOp
     *        checkout} operation will be performed against the newly created branch. If the check
     *        out failed for any reason the {@link CheckoutException} will be propagated back to the
     *        caller, although the branch is guaranteed to be created and could be retrieved through
     *        a {@link RefParse ref-parse} op.
     */
    public BranchCreateOp setAutoCheckout(boolean checkout) {
        this.checkout = checkout;
        return this;
    }

    public Ref call() {
        checkState(branchName != null, "branch name was not provided");
        final String branchRefPath = Ref.HEADS_PREFIX + branchName;
        checkArgument(force || !command(RefParse.class).setName(branchRefPath).call().isPresent(),
                "A branch named '" + branchName + "' already exists.");

        Optional<Ref> branchRef;
        if (orphan) {
            branchRef = command(UpdateRef.class).setName(branchRefPath).setNewValue(ObjectId.NULL)
                    .call();
        } else {
            final String branchOrigin = Optional.fromNullable(commit_ish).or(Ref.HEAD);

            final ObjectId branchOriginCommitId = resolveOriginCommitId(branchOrigin);

            branchRef = command(UpdateRef.class).setName(branchRefPath)
                    .setNewValue(branchOriginCommitId).call();
            checkState(branchRef.isPresent());
        }

        if (checkout) {
            command(CheckoutOp.class).setSource(branchRefPath).call();
        }
        return branchRef.get();
    }

    private ObjectId resolveOriginCommitId(String branchOrigin) {
        Optional<Ref> ref = command(RefParse.class).setName(branchOrigin).call();
        if (ref.isPresent()) {
            ObjectId commitId = ref.get().getObjectId();
            checkArgument(!commitId.isNull(), branchOrigin
                    + " has no commits yet, branch cannot be created.");
            return commitId;
        }
        Optional<ObjectId> objectId = command(RevParse.class).setRefSpec(branchOrigin).call();
        checkArgument(objectId.isPresent(), branchOrigin
                + " does not resolve to a repository object");

        ObjectId commitId = objectId.get();
        TYPE objectType = command(ResolveObjectType.class).setObjectId(commitId).call();
        checkArgument(TYPE.COMMIT.equals(objectType), branchOrigin
                + " does not resolve to a commit: " + objectType);

        return commitId;
    }
}
