/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;

import com.google.common.base.Supplier;
import com.google.inject.Inject;

/**
 * A base class for {@link StagingDatabase}s that forward all {@link ObjectDatabase} change methods
 * to the corresponding "staging" object database, and delegates all query methods to both databases
 * as appropriate.
 */
public abstract class ForwardingStagingDatabase implements StagingDatabase {

    protected ObjectDatabase repositoryDb;

    protected volatile ObjectDatabase stagingDb;

    private Supplier<? extends ObjectDatabase> repositoryDbSupplier;

    private Supplier<? extends ObjectDatabase> stagingDbSupplier;

    /**
     * @param repositoryDb the repository reference database, used to get delegate read operations
     *        to for objects not found here
     */
    @Inject
    public ForwardingStagingDatabase(final Supplier<? extends ObjectDatabase> repositoryDb,
            final Supplier<? extends ObjectDatabase> stagingDb) {
        this.repositoryDbSupplier = repositoryDb;
        this.stagingDbSupplier = stagingDb;
    }

    // /////////////////////////////////////////

    private synchronized ObjectDatabase getStagingDb() {
        if (stagingDb == null) {
            stagingDb = stagingDbSupplier.get();
            repositoryDb = repositoryDbSupplier.get();
        }
        return stagingDb;
    }

    @Override
    public void open() {
        getStagingDb().open();
    }

    @Override
    public boolean isOpen() {
        return getStagingDb().isOpen();
    }

    @Override
    public void close() {
        getStagingDb().close();
    }

    @Override
    public RevTree getTree(ObjectId id) {
        return get(id, RevTree.class);
    }

    @Override
    public RevFeature getFeature(ObjectId id) {
        return get(id, RevFeature.class);

    }

    @Override
    public RevFeatureType getFeatureType(ObjectId id) {
        return get(id, RevFeatureType.class);
    }

    @Override
    public RevCommit getCommit(ObjectId id) {
        return get(id, RevCommit.class);
    }

    @Override
    public RevTag getTag(ObjectId id) {
        return get(id, RevTag.class);
    }

    @Override
    public boolean put(RevObject object) {
        // if (repositoryDb.exists(object.getId())) {
        // return false;
        // }
        return stagingDb.put(object);
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids) {
        return getAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public Iterator<RevObject> getAll(final Iterable<ObjectId> ids, final BulkOpListener listener) {
        return StagingDbCompositionHelper.getAll(repositoryDb, stagingDb, ids, listener);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects) {
        stagingDb.putAll(objects);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects, BulkOpListener listener) {
        stagingDb.putAll(objects, listener);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids) {
        return deleteAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, BulkOpListener listener) {
        return stagingDb.deleteAll(ids, listener);
    }

    @Override
    public boolean exists(ObjectId id) {
        boolean exists = stagingDb.exists(id) || repositoryDb.exists(id);
        return exists;
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        Set<ObjectId> lookUp = new HashSet<ObjectId>(stagingDb.lookUp(partialId));
        lookUp.addAll(repositoryDb.lookUp(partialId));
        return new ArrayList<ObjectId>(lookUp);
    }

    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> type) {
        T obj = stagingDb.getIfPresent(id, type);
        if (null == obj) {
            obj = repositoryDb.get(id, type);
        }
        return obj;
    }

    @Override
    @Nullable
    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> clazz)
            throws IllegalArgumentException {
        T obj = stagingDb.getIfPresent(id, clazz);
        if (null == obj) {
            obj = repositoryDb.getIfPresent(id, clazz);
        }
        return obj;
    }

    @Override
    public RevObject get(ObjectId id) {
        RevObject obj = stagingDb.getIfPresent(id);
        if (null == obj) {
            obj = repositoryDb.get(id);
        }
        return obj;
    }

    @Override
    public @Nullable
    RevObject getIfPresent(ObjectId id) {
        RevObject obj = stagingDb.getIfPresent(id);
        if (null == obj) {
            obj = repositoryDb.getIfPresent(id);
        }
        return obj;
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return stagingDb.newObjectInserter();
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return stagingDb.delete(objectId);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), stagingDb.toString());
    }
}
