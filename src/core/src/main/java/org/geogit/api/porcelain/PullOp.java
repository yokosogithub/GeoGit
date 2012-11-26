/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;

/**
 * Incorporates changes from a remote repository into the current branch.
 */
public class PullOp extends AbstractGeoGitOp<Void> {

    private boolean all;

    private boolean rebase;

    private Supplier<Optional<Remote>> remote;

    private List<String> refSpecs = new ArrayList<String>();

    /**
     * Constructs a new {@code PullOp}.
     */
    @Inject
    public PullOp() {
    }

    /**
     * @param all if {@code true}, pull from all remotes.
     * @return {@code this}
     */
    public PullOp setAll(final boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @param rebase if {@code true}, perform a rebase on the remote branch instead of a merge
     * @return {@code this}
     */
    public PullOp setRebase(final boolean rebase) {
        this.rebase = rebase;
        return this;
    }

    /**
     * @param refSpec the refspec of a remote branch
     * @return {@code this}
     */
    public PullOp addRefSpec(final String refSpec) {
        refSpecs.add(refSpec);
        return this;
    }

    /**
     * @param remoteName the name or URL of a remote repository to fetch from
     * @return {@code this}
     */
    public PullOp setRemote(final String remoteName) {
        Preconditions.checkNotNull(remoteName);
        return setRemote(command(RemoteResolve.class).setName(remoteName));
    }

    /**
     * @param remoteSupplier the remote repository to fetch from
     * @return {@code this}
     */
    public PullOp setRemote(Supplier<Optional<Remote>> remoteSupplier) {
        Preconditions.checkNotNull(remoteSupplier);
        remote = remoteSupplier;

        return this;
    }

    /**
     * Executes the pull operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public Void call() {

        if (remote == null) {
            setRemote("origin");
        }

        Optional<Remote> remoteRepo = remote.get();

        Preconditions.checkArgument(remoteRepo.isPresent(), "Remote could not be resolved.");
        getProgressListener().started();

        command(FetchOp.class).addRemote(remote).setAll(all).setProgressListener(subProgress(80.f))
                .call();

        if (refSpecs.size() > 0) {
            throw new UnsupportedOperationException("Pull does not currently handle ref specs.");
        } else {

            // pull current branch
            final Optional<Ref> currHead = command(RefParse.class).setName(Ref.HEAD).call();
            Preconditions.checkState(currHead.isPresent(), "Repository has no HEAD, can't pull.");
            Preconditions.checkState(currHead.get() instanceof SymRef,
                    "Can't pull from detached HEAD");
            final SymRef headRef = (SymRef) currHead.get();
            final String currentBranch = Ref.localName(headRef.getTarget());

            String remoteBranch = Ref.REMOTES_PREFIX + remoteRepo.get().getName() + "/"
                    + currentBranch;

            Optional<Ref> upstream = command(RefParse.class).setName(remoteBranch).call();

            Preconditions.checkState(upstream.isPresent(),
                    "Cannot pull into current branch, no common ancestor was found.");

            if (rebase) {
                command(RebaseOp.class)
                        .setUpstream(Suppliers.ofInstance(upstream.get().getObjectId()))
                        .setProgressListener(subProgress(20.f)).call();
            } else {
                throw new UnsupportedOperationException("Merge pull is current unsupported.");
            }

        }
        getProgressListener().complete();

        return null;
    }
}
