/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geogit.api.TreeVisitor;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.RevParse;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RawObjectWriter;
import org.geogit.storage.StagingDatabase;
import org.geotools.util.NullProgressListener;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * The Index keeps track of the changes not yet committed to the repository.
 * <p>
 * The Index uses an {@link StagingDatabase object database} as storage for the staged and unstaged
 * changes. This allows for really large operations not to eat up too much heap, and also works
 * better and allows for easier implementation of operations that need to manipulate the index.
 * <p>
 * The Index database is a composite of its own ObjectDatabase and the repository's. Object look ups
 * against the index first search on the index db, and if not found defer to the repository object
 * db.
 * <p>
 * The index holds references to two trees of its own, one for the staged changes and one for the
 * unstaged ones. Modifications to the working tree shall update the Index unstaged changes tree
 * through the {@link #inserted(Iterator) inserted} and {@link #deleted(String...) deleted} methods
 * (an object update is just another insert as far as GeoGit is concerned).
 * <p>
 * Marking unstaged changes to be committed is made through the {@link #stage(String...)} method.
 * <p>
 * Internally, finding out what changes are unstaged is a matter of comparing (through a diff tree
 * walk) the unstaged changes tree and the staged changes tree. And finding out what changes are
 * staged to be committed is performed through a diff tree walk comparing the staged changes tree
 * and the repository's head tree (or any other repository tree reference given to
 * {@link #writeTree(NodeRef)}).
 * <p>
 * When staged changes are to be committed to the repository, the {@link #writeTree(NodeRef)} method
 * shall be called with a reference to the repository root tree that the staged changes tree is to
 * be compared against (usually the HEAD tree ref).
 * 
 * @author Gabriel Roldan
 * 
 */
public class Index implements StagingArea {

    @Inject
    private Repository repository;

    @Inject
    private StagingDatabase indexDatabase;

    public Index() {
    }

    @Override
    public StagingDatabase getDatabase() {
        return indexDatabase;
    }

    @Override
    public boolean deleted(final String path) throws Exception {
        checkValidPath(path);

        NodeRef oldEntry = null;

        Optional<NodeRef> unstagedEntry = indexDatabase.findUnstaged(path);
        if (unstagedEntry.isPresent()) {
            oldEntry = unstagedEntry.get();
        } else {
            Optional<NodeRef> stagedEntry = indexDatabase.findStaged(path);
            if (stagedEntry.isPresent()) {
                oldEntry = stagedEntry.get();
            } else {
                Optional<NodeRef> repoEntry = repository.getRootTreeChild(path);
                if (repoEntry.isPresent()) {
                    oldEntry = repoEntry.get();
                }
            }
        }

        if (oldEntry != null) {
            NodeRef deletedEntry;
            if (oldEntry instanceof SpatialRef) {
                SpatialRef sr = (SpatialRef) oldEntry;
                deletedEntry = new SpatialRef(sr.getPath(), ObjectId.NULL, sr.getMetadataId(),
                        sr.getType(), sr.getBounds());
            } else {
                deletedEntry = new NodeRef(oldEntry.getPath(), ObjectId.NULL,
                        oldEntry.getMetadataId(), oldEntry.getType());
            }
            indexDatabase.putUnstaged(deletedEntry);
            return true;
        }
        return false;
    }

    @Override
    public NodeRef insert(final String parentTreePath, final RevFeature feature) throws Exception {
        checkValidPath(parentTreePath);
        checkNotNull(feature);

        ObjectSerialisingFactory serialFactory = getDatabase().getSerialFactory();
        final ObjectWriter<?> featureWriter = serialFactory.createFeatureWriter(feature);

        final RevFeatureType featureType = feature.getFeatureType();
        final ObjectWriter<RevFeatureType> featureTypeWriter = serialFactory
                .createFeatureTypeWriter(featureType);

        final BoundingBox bounds = feature.getBounds();
        final String nodePath = NodeRef.appendChild(parentTreePath, feature.getFeatureId());

        final ObjectId objectId = indexDatabase.put(featureWriter);
        final ObjectId metadataId;
        if (featureType.getId().isNull()) {
            metadataId = indexDatabase.put(featureTypeWriter);
        } else {
            metadataId = featureType.getId();
        }

        NodeRef newObject;
        if (bounds == null) {
            newObject = new NodeRef(nodePath, objectId, metadataId, TYPE.FEATURE);
        } else {
            newObject = new SpatialRef(nodePath, objectId, metadataId, TYPE.FEATURE, bounds);
        }

        indexDatabase.putUnstaged(newObject);

        return newObject;
    }

    @Override
    public void insert(final String parentTreePath, final Iterator<RevFeature> objects,
            final ProgressListener progress, final @Nullable Integer size,
            @Nullable final List<NodeRef> target) throws Exception {

        checkNotNull(objects);
        checkNotNull(progress);
        checkArgument(size == null || size.intValue() > 0);

        RevFeature revFeature;
        int count = 0;

        progress.started();
        while (objects.hasNext()) {
            count++;
            if (progress.isCanceled()) {
                return;
            }
            if (size != null) {
                progress.progress((float) (count * 100) / size.intValue());
            }

            revFeature = objects.next();
            NodeRef objectRef = insert(parentTreePath, revFeature);
            if (target != null) {
                target.add(objectRef);
            }
        }
        progress.complete();
    }

    @Override
    public void stage(final ProgressListener progress, final @Nullable String pathFilter)
            throws Exception {
        final int numChanges = indexDatabase.countUnstaged(pathFilter);
        int i = 0;
        progress.started();
        // System.err.println("staging with path: " + path2 + ". Matches: " + numChanges);
        Iterator<NodeRef> unstaged = indexDatabase.getUnstaged(pathFilter);
        while (unstaged.hasNext()) {
            i++;
            progress.progress((float) (i * 100) / numChanges);

            NodeRef entry = unstaged.next();
            indexDatabase.stage(entry);
        }
        progress.complete();
    }

    @Override
    public void renamed(final String fromPath, final String toPath) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void reset() {
        indexDatabase.reset();
    }

    @Override
    public ObjectId writeTree(Ref targetRef) throws Exception {
        return writeTree(targetRef, new NullProgressListener());
    }

    /**
     * REVISIT: the Ref should be resolved by the caller and we should get the actual
     * {@link RevTree} here instead
     */
    @Override
    public ObjectId writeTree(final Ref targetRef, final ProgressListener progress)
            throws Exception {

        checkNotNull(targetRef, "null targetRef");
        checkNotNull(progress, "null ProgressListener. Use new NullProgressListener() instead");

        // resolve target ref to the target root tree id
        final Ref targetRootTreeRef;

        final ObjectId targetObjectId = repository.command(RevParse.class)
                .setRefSpec(targetRef.getName()).call();

        if (targetObjectId.isNull()) {
            targetRootTreeRef = new Ref(targetRef.getName(), ObjectId.NULL, TYPE.TREE);
        } else {
            final TYPE type = repository.command(ResolveObjectType.class)
                    .setObjectId(targetObjectId).call();

            if (TYPE.TREE.equals(type)) {
                targetRootTreeRef = targetRef;
            } else if (TYPE.COMMIT.equals(type)) {
                RevCommit commit = repository.getCommit(targetObjectId);
                ObjectId targetTeeId = commit.getTreeId();
                targetRootTreeRef = new Ref(targetRef.getName(), targetTeeId, TYPE.TREE);
            } else {
                throw new IllegalStateException("target ref is not a commit nor a tree");
            }
        }

        final ObjectId toTreeId = targetRootTreeRef.getObjectId();
        return writeTree(toTreeId, progress);
    }

    @Override
    public ObjectId writeTree(final ObjectId targetTreeId, final ProgressListener progress)
            throws Exception {

        final ObjectDatabase repositoryDatabase = repository.getObjectDatabase();

        final RevTree oldRoot = repository.getTree(targetTreeId);

        String pathFilter = null;
        final int numChanges = indexDatabase.countStaged(pathFilter);
        if (numChanges == 0) {
            return targetTreeId;
        }
        if (progress.isCanceled()) {
            return null;
        }

        Iterator<NodeRef> staged = indexDatabase.getStaged(pathFilter);

        Map<String, MutableTree> changedTrees = Maps.newHashMap();

        NodeRef ref;
        int i = 0;
        while (staged.hasNext()) {
            progress.progress((float) (++i * 100) / numChanges);
            if (progress.isCanceled()) {
                return null;
            }

            ref = staged.next();

            final String parentPath = NodeRef.parentPath(ref.getPath());

            MutableTree parentTree = parentPath == null ? null : changedTrees.get(parentPath);

            if (parentPath != null && parentTree == null) {
                parentTree = repositoryDatabase.getOrCreateSubTree(oldRoot, parentPath);
                changedTrees.put(parentPath, parentTree);
            }

            final boolean isDelete = ObjectId.NULL.equals(ref.getObjectId());
            if (isDelete) {
                parentTree.remove(ref.getPath());
            } else {
                deepMove(ref, indexDatabase, repositoryDatabase);
                parentTree.put(ref);
            }
        }

        if (progress.isCanceled()) {
            return null;
        }
        // now write back all changed trees
        ObjectId newTargetRootId = targetTreeId;
        for (Map.Entry<String, MutableTree> e : changedTrees.entrySet()) {
            String treePath = e.getKey();
            MutableTree tree = e.getValue();
            RevTree newRoot = repository.getTree(newTargetRootId);
            newTargetRootId = repositoryDatabase.writeBack(newRoot.mutable(), tree, treePath);
        }

        indexDatabase.removeStaged(pathFilter);

        progress.complete();

        return newTargetRootId;
    }

    /**
     * Transfers the object referenced by {@code objectRef} from the given object database to the
     * given objectInserter as well as any child object if {@code objectRef} references a tree.
     * 
     * @param newObject
     * @param repositoryObjectInserter
     * @throws Exception
     */
    private void deepMove(final NodeRef objectRef, final ObjectDatabase from,
            final ObjectDatabase to) throws Exception {

        final ObjectId objectId = objectRef.getObjectId();
        final ObjectId metadataId = objectRef.getMetadataId();
        moveObject(objectId, from, to, true);
        if (!metadataId.isNull()) {
            moveObject(metadataId, from, to, false);
        }

        if (TYPE.TREE.equals(objectRef.getType())) {
            RevTree tree = from.get(objectId, repository.newRevTreeReader(from));
            tree.accept(new TreeVisitor() {

                @Override
                public boolean visitEntry(final NodeRef ref) {
                    try {
                        deepMove(ref, from, to);
                    } catch (Exception e) {
                        Throwables.propagate(e);
                    }
                    return true;
                }

                @Override
                public boolean visitSubTree(int bucket, ObjectId treeId) {
                    return true;
                }
            });
        }
    }

    private void moveObject(final ObjectId objectId, final ObjectDatabase from,
            final ObjectDatabase to, boolean failIfNotPresent) throws IOException {

        if (to.exists(objectId)) {
            from.delete(objectId);
            return;
        }

        final InputStream raw = from.getRaw(objectId);
        final ObjectId insertedId;
        try {
            insertedId = to.put(new RawObjectWriter(raw));
            from.delete(objectId);

            checkState(objectId.equals(insertedId));
            checkState(to.exists(insertedId));

        } finally {
            raw.close();
        }
    }

    private void checkValidPath(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("empty path");
        }
        if (path.charAt(path.length() - 1) == NodeRef.PATH_SEPARATOR) {
            throw new IllegalArgumentException("path cannot end with path separator: " + path);
        }
    }
}
