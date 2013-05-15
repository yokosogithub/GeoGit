package org.geogit.storage;

import static org.geogit.api.Ref.append;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.geogit.api.GeogitTransaction;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.merge.Conflict;

import com.google.common.base.Optional;

public class TransactionStagingDatabase implements StagingDatabase {

    private final StagingDatabase database;

    private final String txNamespace;

    public TransactionStagingDatabase(final StagingDatabase database, final UUID transactionId) {
        this.database = database;
        this.txNamespace = append(
                append(GeogitTransaction.TRANSACTIONS_NAMESPACE, transactionId.toString()),
                "conflicts");
    }

    @Override
    public void open() {
        database.open();
    }

    @Override
    public boolean isOpen() {
        return database.isOpen();
    }

    @Override
    public void close() {
        database.close();

    }

    @Override
    public boolean exists(ObjectId id) {
        return database.exists(id);
    }

    @Override
    public InputStream getRaw(ObjectId id) {
        return database.getRaw(id);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        return database.lookUp(partialId);
    }

    @Override
    public RevObject get(ObjectId id) throws IllegalArgumentException {
        return database.get(id);
    }

    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> type) throws IllegalArgumentException {
        return database.get(id, type);
    }

    @Override
    public @Nullable
    RevObject getIfPresent(ObjectId id) {
        return database.getIfPresent(id);
    }

    @Override
    public @Nullable
    <T extends RevObject> T getIfPresent(ObjectId id, Class<T> type)
            throws IllegalArgumentException {
        return database.getIfPresent(id, type);
    }

    @Override
    public RevTree getTree(ObjectId id) {
        return database.getTree(id);
    }

    @Override
    public RevFeature getFeature(ObjectId id) {
        return database.getFeature(id);
    }

    @Override
    public RevFeatureType getFeatureType(ObjectId id) {
        return database.getFeatureType(id);
    }

    @Override
    public RevCommit getCommit(ObjectId id) {
        return database.getCommit(id);
    }

    @Override
    public RevTag getTag(ObjectId id) {
        return database.getTag(id);
    }

    @Override
    public <T extends RevObject> boolean put(T object) {
        return database.put(object);
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return database.newObjectInserter();
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return database.delete(objectId);
    }

    @Override
    public boolean put(ObjectId objectId, InputStream raw) {
        return database.put(objectId, raw);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects) {
        database.putAll(objects);
    }

    @Override
    public Optional<Conflict> getConflict(@Nullable String namespace, String st) {
        return database.getConflict(txNamespace, st);
    }

    @Override
    public List<Conflict> getConflicts(@Nullable String namespace, @Nullable String pathFilter) {
        return database.getConflicts(txNamespace, pathFilter);
    }

    @Override
    public void addConflict(@Nullable String namespace, Conflict conflict) {
        database.addConflict(txNamespace, conflict);
    }

    @Override
    public void removeConflict(@Nullable String namespace, String path) {
        database.removeConflict(txNamespace, path);
    }

    @Override
    public void removeConflicts(@Nullable String namespace) {
        database.removeConflicts(txNamespace);
    }

}
