package org.geogit.remote;

import java.io.File;
import java.io.IOException;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectWriter;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;

public class LocalRemoteRepo implements IRemoteRepo {

    private GeoGIT remoteGeoGit;

    private Injector injector;

    private File workingDirectory;

    public LocalRemoteRepo(Injector injector, File workingDirectory) {
        this.injector = injector;
        this.workingDirectory = workingDirectory;
    }

    @Override
    public void open() throws IOException {
        remoteGeoGit = new GeoGIT(injector, workingDirectory);
        remoteGeoGit.getRepository();

    }

    @Override
    public void close() throws IOException {
        remoteGeoGit.close();

    }

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

    @Override
    public void fetchNewData(Repository localRepository, Ref ref) {
        ObjectInserter objectInserter = localRepository.newObjectInserter();
        walkCommit(ref.getObjectId(), localRepository, objectInserter);
    }

    private void walkCommit(ObjectId commitId, Repository localRepository,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (localRepository.getObjectDatabase().exists(commitId)) {
            return;
        }

        Optional<RevObject> object = remoteGeoGit.command(RevObjectParse.class)
                .setObjectId(commitId).call();
        if (object.isPresent() && object.get().getType().equals(TYPE.COMMIT)) {
            RevCommit commit = (RevCommit) object.get();
            walkTree(commit.getTreeId(), localRepository, objectInserter);

            objectInserter.insert(commit.getId(), localRepository.newCommitWriter(commit));
            for (ObjectId parentCommit : commit.getParentIds()) {
                walkCommit(parentCommit, localRepository, objectInserter);
            }
        }
    }

    private void walkTree(ObjectId treeId, Repository localRepository, ObjectInserter objectInserter) {
        // See if we already have it
        if (localRepository.getObjectDatabase().exists(treeId)) {
            return;
        }

        Optional<RevObject> object = remoteGeoGit.command(RevObjectParse.class).setObjectId(treeId)
                .call();
        if (object.isPresent() && object.get().getType().equals(TYPE.TREE)) {
            RevTree tree = (RevTree) object.get();

            objectInserter.insert(tree.getId(), localRepository.newRevTreeWriter(tree));
            // walk subtrees
            if (tree.buckets().isPresent()) {
                for (ObjectId bucketId : tree.buckets().get().values()) {
                    walkTree(bucketId, localRepository, objectInserter);
                }
            }
            // get new objects
            if (tree.children().isPresent()) {
                for (NodeRef ref : tree.children().get()) {
                    moveObject(ref.getObjectId(), localRepository, objectInserter);
                    if (!ref.getMetadataId().isNull()) {
                        moveObject(ref.getMetadataId(), localRepository, objectInserter);
                    }
                }
            }
        }
    }

    private void moveObject(ObjectId objectId, Repository localRepository,
            ObjectInserter objectInserter) {
        // See if we already have it
        if (localRepository.getObjectDatabase().exists(objectId)) {
            return;
        }

        Optional<RevObject> childObject = remoteGeoGit.command(RevObjectParse.class)
                .setObjectId(objectId).call();
        if (childObject.isPresent()) {
            ObjectWriter<? extends RevObject> objectWriter = null;
            switch (childObject.get().getType()) {
            case TREE:
                walkTree(objectId, localRepository, objectInserter);
                objectWriter = localRepository.newRevTreeWriter((RevTree) childObject.get());
                break;
            case FEATURE:
                objectWriter = localRepository.newFeatureWriter((RevFeature) childObject.get());
                break;
            case FEATURETYPE:
                objectWriter = localRepository.newFeatureTypeWriter((RevFeatureType) childObject
                        .get());
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
