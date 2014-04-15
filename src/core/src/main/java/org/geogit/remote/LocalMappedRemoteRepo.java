/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.CommitBuilder;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.api.plumbing.WriteTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.repository.Repository;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;

/**
 * An implementation of a remote repository that exists on the local machine.
 * 
 * @see IRemoteRepo
 */
public class LocalMappedRemoteRepo extends AbstractMappedRemoteRepo {

    private GeoGIT remoteGeoGit;

    private Injector injector;

    private File workingDirectory;

    /**
     * Constructs a new {@code MappedLocalRemoteRepo} with the given parameters.
     * 
     * @param injector the Guice injector for the new repository
     * @param workingDirectory the directory of the remote repository
     */
    public LocalMappedRemoteRepo(Injector injector, File workingDirectory,
            Repository localRepository) {
        super(localRepository);
        this.injector = injector;
        this.workingDirectory = workingDirectory;
    }

    /**
     * @param geogit manually set a geogit for this remote repository
     */
    public void setGeoGit(GeoGIT geogit) {
        this.remoteGeoGit = geogit;
    }

    /**
     * Opens the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void open() throws IOException {
        if (remoteGeoGit == null) {
            remoteGeoGit = new GeoGIT(injector, workingDirectory);
            remoteGeoGit.getRepository();
        }

    }

    /**
     * Closes the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        remoteGeoGit.close();

    }

    /**
     * @return the remote's HEAD {@link Ref}.
     */
    @Override
    public Ref headRef() {
        final Optional<Ref> currHead = remoteGeoGit.command(RefParse.class).setName(Ref.HEAD)
                .call();
        Preconditions.checkState(currHead.isPresent(), "Remote repository has no HEAD.");
        return currHead.get();
    }

    /**
     * List the remote's {@link Ref refs}.
     * 
     * @param getHeads whether to return refs in the {@code refs/heads} namespace
     * @param getTags whether to return refs in the {@code refs/tags} namespace
     * @return an immutable set of refs from the remote
     */
    @Override
    public ImmutableSet<Ref> listRefs(final boolean getHeads, final boolean getTags) {
        Predicate<Ref> filter = new Predicate<Ref>() {
            @Override
            public boolean apply(Ref input) {
                boolean keep = false;
                if (getHeads) {
                    keep = input.getName().startsWith(Ref.HEADS_PREFIX);
                }
                if (getTags) {
                    keep = keep || input.getName().startsWith(Ref.TAGS_PREFIX);
                }
                return keep;
            }
        };

        ImmutableSet<Ref> remoteRefs = remoteGeoGit.command(ForEachRef.class).setFilter(filter)
                .call();

        // Translate the refs to their mapped values.
        ImmutableSet.Builder<Ref> builder = new ImmutableSet.Builder<Ref>();
        for (Ref remoteRef : remoteRefs) {
            Ref newRef = remoteRef;
            if (!(newRef instanceof SymRef)
                    && localRepository.getGraphDatabase().exists(remoteRef.getObjectId())) {
                ObjectId mappedCommit = localRepository.getGraphDatabase().getMapping(
                        remoteRef.getObjectId());
                if (mappedCommit != null) {
                    newRef = new Ref(remoteRef.getName(), mappedCommit);
                }
            }
            builder.add(newRef);
        }
        return builder.build();
    }

    /**
     * Delete the given refspec from the remote repository.
     * 
     * @param refspec the refspec to delete
     */
    @Override
    public void deleteRef(String refspec) {
        remoteGeoGit.command(UpdateRef.class).setName(refspec).setDelete(true).call();
    }

    /**
     * Gets the remote ref that matches the provided ref spec.
     * 
     * @param refspec the refspec to parse
     * @return the matching {@link Ref} or {@link Optional#absent()} if the ref could not be found
     */
    @Override
    protected Optional<Ref> getRemoteRef(String refspec) {
        return remoteGeoGit.command(RefParse.class).setName(refspec).call();
    }

    /**
     * Updates the remote ref that matches the given refspec.
     * 
     * @param refspec the ref to update
     * @param commitId the new value of the ref
     * @param delete if true, the remote ref will be deleted
     * @return the updated ref
     */
    @Override
    protected Ref updateRemoteRef(String refspec, ObjectId commitId, boolean delete) {
        Ref updatedRef = remoteGeoGit.command(UpdateRef.class).setName(refspec)
                .setNewValue(commitId).setDelete(delete).call().get();

        Ref remoteHead = headRef();
        if (remoteHead instanceof SymRef) {
            if (((SymRef) remoteHead).getTarget().equals(updatedRef.getName())) {
                remoteGeoGit.command(UpdateSymRef.class).setName(Ref.HEAD)
                        .setNewValue(updatedRef.getName()).call();
                RevCommit commit = remoteGeoGit.getRepository().getCommit(commitId);
                remoteGeoGit.getRepository().getWorkingTree().updateWorkHead(commit.getTreeId());
                remoteGeoGit.getRepository().getIndex().updateStageHead(commit.getTreeId());
            }
        }
        return updatedRef;
    }

    /**
     * This function takes all of the changes introduced by a commit on the sparse repository and
     * creates a new commit on the full repository with those changes.
     * 
     * @param commitId the commit id of commit from the sparse repository
     * @param from the sparse repository
     * @param to the full repository
     */
    protected void pushSparseCommit(ObjectId commitId) {
        Repository from = localRepository;
        Repository to = remoteGeoGit.getRepository();
        Optional<RevObject> object = from.command(RevObjectParse.class).setObjectId(commitId)
                .call();
        if (object.isPresent() && object.get().getType().equals(TYPE.COMMIT)) {
            RevCommit commit = (RevCommit) object.get();
            ObjectId parent = ObjectId.NULL;
            List<ObjectId> newParents = new LinkedList<ObjectId>();
            for (int i = 0; i < commit.getParentIds().size(); i++) {
                ObjectId parentId = commit.getParentIds().get(i);
                if (i != 0) {
                    Optional<ObjectId> commonAncestor = from.getGraphDatabase()
                            .findLowestCommonAncestor(commit.getParentIds().get(0), parentId);
                    if (commonAncestor.isPresent()) {
                        if (from.getGraphDatabase().isSparsePath(parentId, commonAncestor.get())) {
                            // This should be the base commit to preserve the sparse changes that
                            // were filtered
                            // out.
                            newParents.add(0, from.getGraphDatabase().getMapping(parentId));
                            continue;
                        }
                    }
                }
                newParents.add(from.getGraphDatabase().getMapping(parentId));
            }
            if (newParents.size() > 0) {
                parent = from.getGraphDatabase().getMapping(newParents.get(0));
            }
            Iterator<DiffEntry> diffIter = from.command(DiffOp.class).setNewVersion(commitId)
                    .setOldVersion(parent).setReportTrees(true).call();

            LocalCopyingDiffIterator changes = new LocalCopyingDiffIterator(diffIter, from, to);

            RevTree rootTree = RevTree.EMPTY;

            if (newParents.size() > 0) {
                ObjectId mappedCommit = newParents.get(0);

                Optional<ObjectId> treeId = to.command(ResolveTreeish.class)
                        .setTreeish(mappedCommit).call();
                if (treeId.isPresent()) {
                    rootTree = to.getTree(treeId.get());
                }
            }

            // Create new commit
            ObjectId newTreeId = to.command(WriteTree.class)
                    .setOldRoot(Suppliers.ofInstance(rootTree))
                    .setDiffSupplier(Suppliers.ofInstance((Iterator<DiffEntry>) changes)).call();

            CommitBuilder builder = new CommitBuilder(commit);
            builder.setParentIds(newParents);
            builder.setTreeId(newTreeId);

            RevCommit mapped = builder.build();
            to.getObjectDatabase().put(mapped);

            from.getGraphDatabase().map(commit.getId(), mapped.getId());
            from.getGraphDatabase().map(mapped.getId(), commit.getId());

        }
    }

    /**
     * @return the {@link RepositoryWrapper} for this remote
     */
    @Override
    public RepositoryWrapper getRemoteWrapper() {
        return new LocalRepositoryWrapper(remoteGeoGit.getRepository());
    }

    /**
     * Retrieves an object with the specified id from the remote.
     * 
     * @param objectId the object to get
     * @return the fetched object
     */
    @Override
    protected Optional<RevObject> getObject(ObjectId objectId) {
        return remoteGeoGit.command(RevObjectParse.class).setObjectId(objectId).call();
    }

    /**
     * Gets all of the changes from the target commit that should be applied to the sparse clone.
     * 
     * @param commit the commit to get changes from
     * @return an iterator for changes that match the repository filter
     */
    @Override
    protected FilteredDiffIterator getFilteredChanges(RevCommit commit) {
        ObjectId parent = ObjectId.NULL;
        if (commit.getParentIds().size() > 0) {
            parent = commit.getParentIds().get(0);
        }

        Iterator<DiffEntry> changes = remoteGeoGit.command(DiffOp.class)
                .setNewVersion(commit.getId()).setOldVersion(parent).setReportTrees(true).call();

        return new LocalFilteredDiffIterator(changes, remoteGeoGit.getRepository(),
                localRepository, filter);
    }

    /**
     * Gets the depth of the remote repository.
     * 
     * @return the depth of the repository, or {@link Optional#absent()} if the repository is not
     *         shallow
     */
    @Override
    public Optional<Integer> getDepth() {
        return remoteGeoGit.getRepository().getDepth();
    }
}
