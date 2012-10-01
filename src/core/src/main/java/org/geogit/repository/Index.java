/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.SpatialRef;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.StagingDatabase;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
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

    @Inject
    private ObjectSerialisingFactory serialFactory;

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
                Optional<NodeRef> repoEntry = repository.command(FindTreeChild.class)
                        .setChildPath(path).call();
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
    public void reset() {
        indexDatabase.reset();
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
