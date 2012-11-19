/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Updates objects in the working tree to match the version in the index or the specified tree. If
 * no {@link #addPath paths} are given, will also update {@link Ref#HEAD HEAD} to set the specified
 * branch as the current branch, or to the specified commit if the given {@link #setSource origin}
 * is a commit id instead of a branch name, in which case HEAD will be a plain ref instead of a
 * symbolic ref, hence making it a "dettached head".
 */
public class CheckoutOp extends AbstractGeoGitOp<ObjectId> {

    private String branchOrCommit;

    private Set<String> paths;

    private WorkingTree workTree;

    private StagingArea index;

    @Inject
    public CheckoutOp(final WorkingTree workTree, final StagingArea index) {
        this.workTree = workTree;
        this.index = index;
        paths = Sets.newTreeSet();
    }

    public CheckoutOp setSource(final String branchOrCommit) {
        checkNotNull(branchOrCommit);
        this.branchOrCommit = branchOrCommit;
        return this;
    }

    public CheckoutOp addPath(final CharSequence path) {
        checkNotNull(path);
        paths.add(path.toString());
        return this;
    }

    public CheckoutOp addPaths(final Collection<? extends CharSequence> paths) {
        checkNotNull(paths);
        for (CharSequence path : paths) {
            addPath(path);
        }
        return this;
    }

    /**
     * @return the id of the new work tree
     */
    @Override
    public ObjectId call() {
        checkState(branchOrCommit != null || !paths.isEmpty(),
                "No branch, tree, or path were specified");

        Optional<Ref> targetRef = Optional.absent();
        Optional<RevCommit> commit = Optional.absent();
        if (branchOrCommit != null) {
            targetRef = command(RefParse.class).setName(branchOrCommit).call();
            if (targetRef.isPresent()) {
                ObjectId commitId = Optional.of(targetRef.get().getObjectId()).get();
                Optional<RevObject> parsed = command(RevObjectParse.class).setObjectId(commitId)
                        .call();
                checkState(parsed.isPresent());
                checkState(parsed.get() instanceof RevCommit);
                commit = Optional.of((RevCommit) parsed.get());
            } else {
                final Optional<ObjectId> addressed = command(RevParse.class).setRefSpec(
                        branchOrCommit).call();
                checkArgument(addressed.isPresent(), "source '" + branchOrCommit
                        + "' not found in repository");
                if (addressed.isPresent()) {
                    RevObject target;
                    target = command(RevObjectParse.class).setObjectId(addressed.get()).call()
                            .get();
                    checkArgument(target instanceof RevCommit,
                            "source did not resolve to a commit: " + target.getType());
                    commit = Optional.of((RevCommit) target);
                }
            }
        }

        if (commit.isPresent()) {
            // count staged and unstaged changes
            long staged = index.countStaged(null);
            long unstaged = workTree.countUnstaged(null);
            if (staged != 0 || unstaged != 0) {
                throw new UnsupportedOperationException(
                        "Doing a checkout without a clean working tree and index is currently unsupported.");
            }
            // update work tree
            RevCommit revCommit = commit.get();
            ObjectId treeId = revCommit.getTreeId();
            workTree.updateWorkHead(treeId);
            index.updateStageHead(treeId);
            if (targetRef.isPresent()) {
                // update HEAD
                String refName = targetRef.get().getName();
                command(UpdateSymRef.class).setName(Ref.HEAD).setNewValue(refName).call();
            } else {
                // set HEAD to a dettached state
                ObjectId commitId = commit.get().getId();
                command(UpdateRef.class).setName(Ref.HEAD).setNewValue(commitId).call();
            }
            return treeId;
        }
        throw new UnsupportedOperationException("partial checkouts not yet supported");
    }
}
