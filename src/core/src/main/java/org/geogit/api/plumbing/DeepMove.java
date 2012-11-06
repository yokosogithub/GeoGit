/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkState;

import java.io.InputStream;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RawObjectWriter;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Supplier;
import com.google.common.io.Closeables;
import com.google.inject.Inject;

/**
 * Moves the {@link #setObjectRef(Supplier) specified object} from the {@link StagingArea index
 * database} to the permanent {@link ObjectDatabase object database}, including any child reference,
 * or from the repository database to the index database if {@link #setToIndex} is set to
 * {@code true}.
 */
public class DeepMove extends AbstractGeoGitOp<ObjectId> {

    private boolean toIndex;

    private ObjectDatabase odb;

    private StagingDatabase index;

    private Supplier<NodeRef> objectRef;

    private ObjectSerialisingFactory serialFactory;

    /**
     * Constructs a new instance of the {@code DeepMove} operation with the specified parameters.
     * 
     * @param odb the repository object database
     * @param index the staging database
     * @param serialFactory the serialization factory
     */
    @Inject
    public DeepMove(ObjectDatabase odb, StagingDatabase index,
            ObjectSerialisingFactory serialFactory) {
        this.odb = odb;
        this.index = index;
        this.serialFactory = serialFactory;
    }

    /**
     * @param toIndex if {@code true} moves the object from the repository's object database to the
     *        index database instead
     * @return {@code this}
     */
    public DeepMove setToIndex(boolean toIndex) {
        this.toIndex = toIndex;
        return this;
    }

    /**
     * @param objectRef the object to move from the origin database to the destination one
     * @return {@code this}
     */
    public DeepMove setObjectRef(Supplier<NodeRef> objectRef) {
        this.objectRef = objectRef;
        return this;
    }

    /**
     * Executes a deep move using the supplied {@link NodeRef}.
     * 
     * @return the {@link ObjectId} of the moved object
     */
    @Override
    public ObjectId call() {
        ObjectDatabase from = toIndex ? odb : index;
        ObjectDatabase to = toIndex ? index : odb;
        NodeRef ref = objectRef.get();
        deepMove(ref, from, to);
        return null;
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
            final ObjectDatabase to) {

        final ObjectId objectId = objectRef.getObjectId();
        if (TYPE.TREE.equals(objectRef.getType())) {
            RevTree tree = from.get(objectId, serialFactory.createRevTreeReader());
            moveTree(tree, from, to);
        } else {
            moveFeature(objectRef, from, to);
        }
    }

    private void moveFeature(NodeRef objectRef, ObjectDatabase from, ObjectDatabase to) {
        moveObject(objectRef.getObjectId(), from, to, true);

        final ObjectId metadataId = objectRef.getMetadataId();
        if (!metadataId.isNull()) {
            moveObject(metadataId, from, to, false);
        }
    }

    private void moveTree(RevTree tree, ObjectDatabase from, ObjectDatabase to) {
        if (tree.children().isPresent()) {
            for (NodeRef ref : tree.children().get()) {
                deepMove(ref, from, to);
            }
        } else if (tree.buckets().isPresent()) {
            for (ObjectId bucketId : tree.buckets().get().values()) {
                RevTree bucketTree = from.get(bucketId, serialFactory.createRevTreeReader());
                moveTree(bucketTree, from, to);
            }
        }
        moveObject(tree.getId(), from, to, true);
    }

    private void moveObject(final ObjectId objectId, final ObjectDatabase from,
            final ObjectDatabase to, boolean failIfNotPresent) {

        if (to.exists(objectId)) {
            from.delete(objectId);
            return;
        }

        final InputStream raw = from.getRaw(objectId);
        try {
            to.put(objectId, new RawObjectWriter(raw));
            from.delete(objectId);

            checkState(to.exists(objectId));

        } finally {
            Closeables.closeQuietly(raw);
        }
    }

}
