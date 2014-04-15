/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.io.IOException;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.LsRemote;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.ConfigOp.ConfigScope;
import org.geogit.remote.IRemoteRepo;
import org.geogit.remote.RemoteUtils;
import org.geogit.repository.Hints;
import org.geogit.repository.Repository;
import org.geogit.storage.DeduplicationService;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * Clones a remote repository to a given directory.
 * 
 */
public class CloneOp extends AbstractGeoGitOp<Void> {

    private Optional<String> branch = Optional.absent();

    private String repositoryURL;

    private String username = null;

    private String password = null;

    private Optional<Integer> depth = Optional.absent();

    private final Repository repository;

    private final DeduplicationService deduplicationService;

    /**
     * Constructs a new {@code CloneOp}.
     */
    @Inject
    public CloneOp(final Repository repository, final DeduplicationService deduplicationService) {
        this.repository = repository;
        this.deduplicationService = deduplicationService;
    }

    /**
     * @param repositoryURL the URL of the repository to clone
     * @return {@code this}
     */
    public CloneOp setRepositoryURL(final String repositoryURL) {
        this.repositoryURL = repositoryURL;
        return this;
    }

    /**
     * @param username user name for the repository
     * @return {@code this}
     */
    public CloneOp setUserName(String username) {
        this.username = username;
        return this;
    }

    /**
     * @param password password for the repository
     * @return {@code this}
     */
    public CloneOp setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * @param branch the branch to checkout when the clone is complete
     * @return {@code this}
     */
    public CloneOp setBranch(@Nullable String branch) {
        this.branch = Optional.fromNullable(branch);
        return this;
    }

    /**
     * @param depth the depth of the clone, if depth is < 1, then a full clone s performed
     * @return {@code this}
     */
    public CloneOp setDepth(final int depth) {
        if (depth > 0) {
            this.depth = Optional.of(depth);
        }
        return this;
    }

    /**
     * Executes the clone operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    @Override
    public Void call() {
        Preconditions.checkArgument(repositoryURL != null && !repositoryURL.isEmpty(),
                "No repository specified to clone from.");
        if (repository.isSparse()) {
            Preconditions
                    .checkArgument(branch.isPresent(), "No branch specified for sparse clone.");
        }

        getProgressListener().started();
        getProgressListener().setProgress(0.f);

        // Set up origin
        Remote remote = command(RemoteAddOp.class).setName("origin").setURL(repositoryURL)
                .setMapped(repository.isSparse()).setUserName(username).setPassword(password)
                .setBranch(repository.isSparse() ? branch.get() : null).call();

        if (!depth.isPresent()) {
            // See if we are cloning a shallow clone. If so, a depth must be specified.
            Optional<IRemoteRepo> remoteRepo = RemoteUtils.newRemote(
                    GlobalInjectorBuilder.builder.build(Hints.readOnly()), remote, repository,
                    deduplicationService);

            Preconditions.checkState(remoteRepo.isPresent(), "Failed to connect to the remote.");
            IRemoteRepo remoteRepoInstance = remoteRepo.get();
            try {
                remoteRepoInstance.open();
            } catch (IOException e) {
                Throwables.propagate(e);
            }
            try {
                depth = remoteRepoInstance.getDepth();
            } finally {
                try {
                    remoteRepoInstance.close();
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        }

        if (depth.isPresent()) {
            command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setScope(ConfigScope.LOCAL)
                    .setName(Repository.DEPTH_CONFIG_KEY).setValue(depth.get().toString()).call();
        }

        // Fetch remote data
        command(FetchOp.class).setDepth(depth.or(0)).setProgressListener(subProgress(90.f)).call();

        // Set up remote tracking branches
        final ImmutableSet<Ref> remoteRefs = command(LsRemote.class).retrieveTags(false)
                .setRemote(Suppliers.ofInstance(Optional.of(remote))).call();

        boolean emptyRepo = true;

        for (Ref remoteRef : remoteRefs) {
            if (emptyRepo && !remoteRef.getObjectId().isNull()) {
                emptyRepo = false;
            }
            String branchName = remoteRef.localName();
            if (remoteRef instanceof SymRef) {
                continue;
            }
            if (!command(RefParse.class).setName(Ref.HEADS_PREFIX + branchName).call().isPresent()) {
                command(BranchCreateOp.class).setName(branchName)
                        .setSource(remoteRef.getObjectId().toString()).call();
            } else {
                command(UpdateRef.class).setName(Ref.HEADS_PREFIX + branchName)
                        .setNewValue(remoteRef.getObjectId()).call();
            }

            command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setScope(ConfigScope.LOCAL)
                    .setName("branches." + branchName + ".remote").setValue(remote.getName())
                    .call();

            command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setScope(ConfigScope.LOCAL)
                    .setName("branches." + branchName + ".merge")
                    .setValue(Ref.HEADS_PREFIX + remoteRef.localName()).call();
        }
        getProgressListener().setProgress(95.f);

        if (!emptyRepo) {
            // checkout branch
            if (branch.isPresent()) {
                command(CheckoutOp.class).setForce(true).setSource(branch.get()).call();
            } else {
                // checkout the head
                final Optional<Ref> currRemoteHead = command(RefParse.class).setName(
                        Ref.REMOTES_PREFIX + remote.getName() + "/" + Ref.HEAD).call();
                Preconditions.checkState(currRemoteHead.isPresent(), "No remote HEAD.");
                Preconditions.checkState(currRemoteHead.get() instanceof SymRef,
                        "Remote HEAD is detached." + currRemoteHead.get().toString());
                final SymRef remoteHeadRef = (SymRef) currRemoteHead.get();
                final String currentBranch = Ref.localName(remoteHeadRef.getTarget());

                command(CheckoutOp.class).setForce(true).setSource(currentBranch).call();

            }
        }

        getProgressListener().complete();

        return null;
    }
}
