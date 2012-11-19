/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.plumbing.LsRemote;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.remote.IRemoteRepo;
import org.geogit.remote.RemoteUtils;
import org.geogit.repository.Repository;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * Fetches named heads or tags from one or more other repositories, along with the objects necessary
 * to complete them.
 */
public class FetchOp extends AbstractGeoGitOp<Void> {

    private boolean all;

    private boolean prune;

    private List<Remote> remotes = new ArrayList<Remote>();

    private Repository localRepository;

    /**
     * Constructs a new {@code FetchOp}.
     */
    @Inject
    public FetchOp(Repository localRepository) {
        this.localRepository = localRepository;
    }

    /**
     * @param all if {@code true}, fetch from all remotes.
     * @return {@code this}
     */
    public FetchOp setAll(final boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @param prune if {@code true}, remote tracking branches that no longer exist will be removed
     *        locally.
     * @return {@code this}
     */
    public FetchOp setPrune(final boolean prune) {
        this.prune = prune;
        return this;
    }

    /**
     * @param remote the name or URL of a remote repository to fetch from
     * @return {@code this}
     */
    public FetchOp addRemote(final String remoteName) {
        Preconditions.checkNotNull(remoteName);
        return addRemote(command(RemoteResolve.class).setName(remoteName));
    }

    /**
     * @param remote the remote repository to fetch from
     * @return {@code this}
     */
    public FetchOp addRemote(Supplier<Optional<Remote>> remoteSupplier) {
        Preconditions.checkNotNull(remoteSupplier);
        Optional<Remote> remote = remoteSupplier.get();
        Preconditions.checkState(remote.isPresent(), "Remote could not be resolved.");
        remotes.add(remote.get());

        return this;
    }

    /**
     * Executes the fetch operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public Void call() {
        if (all) {
            // Add all remotes to list.
            ImmutableList<Remote> localRemotes = command(RemoteListOp.class).call();
            for (Remote remote : localRemotes) {
                if (!remotes.contains(remote)) {
                    remotes.add(remote);
                }
            }
        } else if (remotes.size() == 0) {
            // If no remotes are specified, default to the origin remote
            Optional<Remote> origin = command(RemoteResolve.class).setName("origin").call();
            if (origin.isPresent()) {
                remotes.add(origin.get());
            }
        }
        Preconditions.checkState(remotes.size() > 0,
                "No remote repository specified.  Please specify a remote name to fetch from.");

        for (Remote remote : remotes) {
            final ImmutableSet<Ref> remoteRemoteRefs = command(LsRemote.class).setRemote(
                    Suppliers.ofInstance(Optional.of(remote))).call();
            final ImmutableSet<Ref> localRemoteRefs = command(LsRemote.class)
                    .retrieveLocalRefs(true).setRemote(Suppliers.ofInstance(Optional.of(remote)))
                    .call();
            final ImmutableSet<Ref> needUpdate = findOutdatedRefs(remote, remoteRemoteRefs,
                    localRemoteRefs);

            if (prune) {
                // Delete local refs that aren't in the remote
                List<Ref> locals = new ArrayList<Ref>();
                for (Ref remoteRef : remoteRemoteRefs) {
                    Optional<Ref> localRef = findLocal(remote, remoteRef, localRemoteRefs);
                    if (localRef.isPresent()) {
                        locals.add(localRef.get());
                    }
                }
                for (Ref localRef : localRemoteRefs) {
                    if (!locals.contains(localRef)) {
                        // Delete the ref
                        command(UpdateRef.class).setDelete(true).setName(localRef.getName()).call();
                    }
                }
            }

            Optional<IRemoteRepo> remoteRepo = RemoteUtils.newRemote(localRepository
                    .getInjectorBuilder().get(), remote);

            if (remoteRepo.isPresent()) {
                try {
                    remoteRepo.get().open();
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
                for (Ref ref : needUpdate) {
                    // Fetch updated data from this ref
                    remoteRepo.get().fetchNewData(localRepository, ref);

                    // Update the ref
                    Optional<Ref> localRef = findLocal(remote, ref, localRemoteRefs);
                    if (localRef.isPresent()) {
                        command(UpdateRef.class).setName(localRef.get().getName())
                                .setNewValue(ref.getObjectId()).call();
                    } else {
                        String splitRef[] = ref.getName().split("/");
                        final String refName = Ref.REMOTES_PREFIX + remote.getName() + "/"
                                + splitRef[splitRef.length - 1];
                        command(UpdateRef.class).setName(refName).setNewValue(ref.getObjectId())
                                .call();
                    }
                }
                try {
                    remoteRepo.get().close();
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }

        }

        return null;
    }

    /**
     * Filters the remote references for the given remote that are not present or outdated in the
     * local repository
     */
    private ImmutableSet<Ref> findOutdatedRefs(Remote remote, ImmutableSet<Ref> remoteRefs,
            ImmutableSet<Ref> localRemoteRefs) {

        ImmutableSet.Builder<Ref> outdatedOrMissing = ImmutableSet.builder();

        for (Ref remoteRef : remoteRefs) {// refs/heads/xxx or refs/tags/yyy, though we don't handle
                                          // tags yet
            Optional<Ref> local = findLocal(remote, remoteRef, localRemoteRefs);
            if (local.isPresent()) {
                if (!local.get().getObjectId().equals(remoteRef.getObjectId())) {
                    outdatedOrMissing.add(remoteRef);
                }
            } else {
                outdatedOrMissing.add(remoteRef);
            }
        }
        return outdatedOrMissing.build();
    }

    /**
     * Finds the corresponding local reference in {@code localRemoteRefs} for the given remote ref
     * 
     * @param remote the remote
     * @param remoteRef a ref in the {@code refs/heads} or {@code refs/tags} namespace as given by
     *        {@link LsRemote} when querying a remote repository
     * @param the list of locally known references of the given remote in the
     *        {@code refs/remotes/<remote name>/} namespace
     */
    private Optional<Ref> findLocal(Remote remote, Ref remoteRef, ImmutableSet<Ref> localRemoteRefs) {
        final String remoteName = remote.getName();
        final String prefix = Ref.REMOTES_PREFIX + remoteName + "/";
        for (Ref localRef : localRemoteRefs) {
            if (localRef.getName().startsWith(prefix)) {
                String splitRef[] = remoteRef.getName().split("/");
                if (localRef.getName().endsWith(splitRef[splitRef.length - 1])) {
                    return Optional.of(localRef);
                }
            }
        }
        return Optional.absent();
    }

}
