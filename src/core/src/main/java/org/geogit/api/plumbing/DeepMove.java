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
import org.geogit.api.TreeVisitor;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RawObjectWriter;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
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
     */
    public DeepMove setToIndex(boolean toIndex) {
        this.toIndex = toIndex;
        return this;
    }

    /**
     * @param objectRef the object to move from the origin database to the destination one
     */
    public DeepMove setObjectRef(Supplier<NodeRef> objectRef) {
        this.objectRef = objectRef;
        return this;
    }

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
        final ObjectId metadataId = objectRef.getMetadataId();
        moveObject(objectId, from, to, true);
        if (!metadataId.isNull()) {
            moveObject(metadataId, from, to, false);
        }

        if (TYPE.TREE.equals(objectRef.getType())) {
            RevTree tree = from.get(objectId, serialFactory.createRevTreeReader(from));
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
