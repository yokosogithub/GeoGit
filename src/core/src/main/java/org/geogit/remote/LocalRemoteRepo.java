package org.geogit.remote;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.geogit.api.GeoGIT;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectWriter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;

/**
 * An implementation of a remote repository that exists on the local machine.
 * 
 * @see IRemoteRepo
 * @author jgarrett
 */
public class LocalRemoteRepo implements IRemoteRepo {

    private GeoGIT remoteGeoGit;

    private Injector injector;

    private File workingDirectory;

    /**
     * Constructs a new {@code LocalRemoteRepo} with the given parameters.
     * 
     * @param injector the Guice injector for the new repository
     * @param workingDirectory the directory of the remote repository
     */
    public LocalRemoteRepo(Injector injector, File workingDirectory) {
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
        return remoteGeoGit.command(ForEachRef.class).setFilter(filter).call();
    }

    /**
     * Fetch all new objects from the specified {@link Ref} from the remote.
     * 
     * @param localRepository the repository to add new objects to
     * @param ref the remote ref that points to new commit data
     */
    @Override
    public void fetchNewData(Repository localRepository, Ref ref) {
        ObjectInserter objectInserter = localRepository.newObjectInserter();
        walkCommit(ref.getObjectId(), remoteGeoGit.getRepository(), localRepository, objectInserter);
    }

    /**
     * Push all new objects from the specified {@link Ref} to the remote.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     */
    @Override
    public void pushNewData(Repository localRepository, Ref ref) {
        ObjectInserter objectInserter = remoteGeoGit.getRepository().newObjectInserter();
        walkCommit(ref.getObjectId(), localRepository, remoteGeoGit.getRepository(), objectInserter);
        remoteGeoGit.command(UpdateRef.class).setName(ref.getName()).setNewValue(ref.getObjectId())
                .call();
    }

    /**
     * Push all new objects from the specified {@link Ref} to the given refspec.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     * @param refspec the refspec to push to
     */
    public void pushNewData(Repository localRepository, Ref ref, String refspec) {
        ObjectInserter objectInserter = remoteGeoGit.getRepository().newObjectInserter();
        walkCommit(ref.getObjectId(), localRepository, remoteGeoGit.getRepository(), objectInserter);
        remoteGeoGit.command(UpdateRef.class).setName(refspec).setNewValue(ref.getObjectId())
                .call();
    }

    /**
     * Delete the given refspec from the remote repository.
     * 
     * @param localRepository the repository to get new objects from
     * @param refspec the refspec to delete
     */
    public void deleteRef(Repository localRepository, String refspec) {
        remoteGeoGit.command(UpdateRef.class).setName(refspec).setDelete(true).call();
    }

    private void walkCommit(ObjectId commitId, Repository from, Repository to,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (to.getObjectDatabase().exists(commitId)) {
            return;
        }

        Optional<RevObject> object = from.command(RevObjectParse.class).setObjectId(commitId)
                .call();
        if (object.isPresent() && object.get().getType().equals(TYPE.COMMIT)) {
            RevCommit commit = (RevCommit) object.get();
            walkTree(commit.getTreeId(), from, to, objectInserter);

            objectInserter.insert(commit.getId(), to.newCommitWriter(commit));
            for (ObjectId parentCommit : commit.getParentIds()) {
                walkCommit(parentCommit, from, to, objectInserter);
            }
        }
    }

    private void walkTree(ObjectId treeId, Repository from, Repository to,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (to.getObjectDatabase().exists(treeId)) {
            return;
        }

        Optional<RevObject> object = from.command(RevObjectParse.class).setObjectId(treeId).call();
        if (object.isPresent() && object.get().getType().equals(TYPE.TREE)) {
            RevTree tree = (RevTree) object.get();

            objectInserter.insert(tree.getId(), to.newRevTreeWriter(tree));
            // walk subtrees
            if (tree.buckets().isPresent()) {
                for (ObjectId bucketId : tree.buckets().get().values()) {
                    walkTree(bucketId, from, to, objectInserter);
                }
            } else {
                // get new objects
                for (Iterator<Node> children = tree.children(); children.hasNext();) {
                    Node ref = children.next();
                    moveObject(ref.getObjectId(), from, to, objectInserter);
                    ObjectId metadataId = ref.getMetadataId().or(ObjectId.NULL);
                    if (!metadataId.isNull()) {
                        moveObject(metadataId, from, to, objectInserter);
                    }
                }
            }
        }
    }

    private void moveObject(ObjectId objectId, Repository from, Repository to,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (to.getObjectDatabase().exists(objectId)) {
            return;
        }

        Optional<RevObject> childObject = from.command(RevObjectParse.class).setObjectId(objectId)
                .call();
        if (childObject.isPresent()) {
            ObjectWriter<? extends RevObject> objectWriter = null;
            switch (childObject.get().getType()) {
            case TREE:
                walkTree(objectId, from, to, objectInserter);
                objectWriter = to.newRevTreeWriter((RevTree) childObject.get());
                break;
            case FEATURE:
                objectWriter = to.newFeatureWriter((RevFeature) childObject.get());
                break;
            case FEATURETYPE:
                objectWriter = to.newFeatureTypeWriter((RevFeatureType) childObject.get());
                break;
            default:
                break;
            }
            if (objectWriter != null) {
                objectInserter.insert(childObject.get().getId(), objectWriter);
            }
        }
    }
}
