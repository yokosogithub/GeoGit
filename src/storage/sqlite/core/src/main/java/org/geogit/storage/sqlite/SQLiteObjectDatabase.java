<<<<<<< .merge_file_0BHKia
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.geogit.storage.sqlite.SQLiteStorage.FORMAT_NAME;
import static org.geogit.storage.sqlite.SQLiteStorage.VERSION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.BulkOpListener;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.datastream.DataStreamSerializationFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 * Base class for SQLite based object database.
 * 
 * @author Justin Deoliveira, Boundless
 * 
 * @param <C> Connection type.
 */
public abstract class SQLiteObjectDatabase<C> implements ObjectDatabase {

    final Platform platform;

    final ConfigDatabase configdb;

    final ObjectSerializingFactory serializer = new DataStreamSerializationFactory();

    C cx;

    public SQLiteObjectDatabase(ConfigDatabase configdb, Platform platform) {
        this.configdb = configdb;
        this.platform = platform;

    }

    @Override
    public void open() {
        if (cx == null) {
            cx = connect(SQLiteStorage.geogitDir(platform));
            init(cx);
        }
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.configure(configdb, FORMAT_NAME, VERSION);
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.verify(configdb, FORMAT_NAME, VERSION);
    }

    @Override
    public boolean isOpen() {
        return cx != null;
    }

    @Override
    public void close() {
        if (cx != null) {
            close(cx);
            cx = null;
        }
    }

    @Override
    public boolean exists(ObjectId id) {
        return has(id.toString(), cx);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        return Lists.newArrayList(transform(search(partialId, cx), StringToObjectId.INSTANCE));
    }

    @Override
    public RevObject get(ObjectId id) throws IllegalArgumentException {
        RevObject obj = getIfPresent(id);
        if (obj == null) {
            throw new NoSuchElementException("No object with id: " + id);
        }

        return obj;
    }

    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> type) throws IllegalArgumentException {
        T obj = getIfPresent(id, type);
        if (obj == null) {
            throw new NoSuchElementException("No object with ids: " + id);
        }

        return obj;
    }

    @Override
    public RevObject getIfPresent(ObjectId id) {
        InputStream bytes = get(id.toString(), cx);
        return readObject(bytes, id);
    }

    @Override
    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> type)
            throws IllegalArgumentException {
        RevObject obj = getIfPresent(id);
        return obj != null ? type.cast(obj) : null;
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
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids) {
        return getAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids, final BulkOpListener listener) {
        return filter(transform(ids, new Function<ObjectId, RevObject>() {
            @Override
            public RevObject apply(ObjectId id) {
                RevObject obj = getIfPresent(id);
                if (obj == null) {
                    listener.notFound(id);
                } else {
                    listener.found(id, null);
                }
                return obj;
            }
        }), Predicates.notNull()).iterator();
    }

    @Override
    public boolean put(RevObject object) {
        String id = object.getId().toString();
        try {
            put(id, writeObject(object), cx);
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize object: " + object);
        }
        return true;
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects) {
        putAll(objects, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects, BulkOpListener listener) {
        while (objects.hasNext()) {
            RevObject obj = objects.next();
            if (put(obj)) {
                listener.inserted(obj.getId(), null);
            }
        }
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return delete(objectId.toString(), cx);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids) {
        return deleteAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, BulkOpListener listener) {
        long count = 0;
        while (ids.hasNext()) {
            ObjectId id = ids.next();
            if (delete(id)) {
                count++;
                listener.deleted(id);
            }
        }
        return count;
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return new ObjectInserter(this);
    }

    /**
     * Reads object from its binary representation as stored in the database.
     */
    protected RevObject readObject(InputStream bytes, ObjectId id) {
        if (bytes == null) {
            return null;
        }

        return serializer.createObjectReader().read(id, bytes);
    }

    /**
     * Writes object to its binary representation as stored in the database.
     */
    protected InputStream writeObject(RevObject object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        serializer.createObjectWriter(object.getType()).write(object, bout);
        return new ByteArrayInputStream(bout.toByteArray());
    }

    /**
     * Opens a database connection, returning the object representing connection state.
     */
    protected abstract C connect(File geogitDir);

    /**
     * Closes a database connection.
     * 
     * @param cx The connection object.
     */
    protected abstract void close(C cx);

    /**
     * Creates the object table with the following schema:
     * 
     * <pre>
     * objects(id:varchar PRIMARY KEY, object:blob)
     * </pre>
     * 
     * Implementations of this method should be prepared to be called multiple times, so must check
     * if the table already exists.
     * 
     * @param cx The connection object.
     */
    protected abstract void init(C cx);

    /**
     * Determines if the object with the specified id exists.
     */
    protected abstract boolean has(String id, C cx);

    /**
     * Searches for objects with ids that match the speciifed partial string.
     * 
     * @param partialId The partial id.
     * 
     * @return Iterable of matches.
     */
    protected abstract Iterable<String> search(String partialId, C cx);

    /**
     * Retrieves the object with the specified id.
     * <p>
     * Must return <code>null</code> if no such object exists.
     * </p>
     */
    protected abstract InputStream get(String id, C cx);

    /**
     * Inserts or updates the object with the specified id.
     */
    protected abstract void put(String id, InputStream obj, C cx);

    /**
     * Deletes the object with the specified id.
     * 
     * @return Flag indicating if object was actually removed.
     */
    protected abstract boolean delete(String id, C cx);
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.geogit.storage.sqlite.SQLiteStorage.FORMAT_NAME;
import static org.geogit.storage.sqlite.SQLiteStorage.VERSION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.BulkOpListener;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.datastream.DataStreamSerializationFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 * Base class for SQLite based object database.
 * 
 * @author Justin Deoliveira, Boundless
 * 
 * @param <C> Connection type.
 */
public abstract class SQLiteObjectDatabase<C> implements ObjectDatabase {

    final Platform platform;

    final ConfigDatabase configdb;

    final ObjectSerializingFactory serializer = DataStreamSerializationFactory.INSTANCE;

    C cx;

    public SQLiteObjectDatabase(ConfigDatabase configdb, Platform platform) {
        this.configdb = configdb;
        this.platform = platform;

    }

    @Override
    public void open() {
        if (cx == null) {
            cx = connect(SQLiteStorage.geogitDir(platform));
            init(cx);
        }
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.configure(configdb, FORMAT_NAME, VERSION);
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.verify(configdb, FORMAT_NAME, VERSION);
    }

    @Override
    public boolean isOpen() {
        return cx != null;
    }

    @Override
    public void close() {
        if (cx != null) {
            close(cx);
            cx = null;
        }
    }

    @Override
    public boolean exists(ObjectId id) {
        return has(id.toString(), cx);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        return Lists.newArrayList(transform(search(partialId, cx), StringToObjectId.INSTANCE));
    }

    @Override
    public RevObject get(ObjectId id) throws IllegalArgumentException {
        RevObject obj = getIfPresent(id);
        if (obj == null) {
            throw new NoSuchElementException("No object with id: " + id);
        }

        return obj;
    }

    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> type) throws IllegalArgumentException {
        T obj = getIfPresent(id, type);
        if (obj == null) {
            throw new NoSuchElementException("No object with ids: " + id);
        }

        return obj;
    }

    @Override
    public RevObject getIfPresent(ObjectId id) {
        InputStream bytes = get(id.toString(), cx);
        return readObject(bytes, id);
    }

    @Override
    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> type)
            throws IllegalArgumentException {
        RevObject obj = getIfPresent(id);
        return obj != null ? type.cast(obj) : null;
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
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids) {
        return getAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids, final BulkOpListener listener) {
        return filter(transform(ids, new Function<ObjectId, RevObject>() {
            @Override
            public RevObject apply(ObjectId id) {
                RevObject obj = getIfPresent(id);
                if (obj == null) {
                    listener.notFound(id);
                } else {
                    listener.found(id, null);
                }
                return obj;
            }
        }), Predicates.notNull()).iterator();
    }

    @Override
    public boolean put(RevObject object) {
        String id = object.getId().toString();
        try {
            put(id, writeObject(object), cx);
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize object: " + object);
        }
        return true;
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects) {
        putAll(objects, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects, BulkOpListener listener) {
        while (objects.hasNext()) {
            RevObject obj = objects.next();
            if (put(obj)) {
                listener.inserted(obj.getId(), null);
            }
        }
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return delete(objectId.toString(), cx);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids) {
        return deleteAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, BulkOpListener listener) {
        long count = 0;
        while (ids.hasNext()) {
            ObjectId id = ids.next();
            if (delete(id)) {
                count++;
                listener.deleted(id);
            }
        }
        return count;
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return new ObjectInserter(this);
    }

    /**
     * Reads object from its binary representation as stored in the database.
     */
    protected RevObject readObject(InputStream bytes, ObjectId id) {
        if (bytes == null) {
            return null;
        }

        return serializer.createObjectReader().read(id, bytes);
    }

    /**
     * Writes object to its binary representation as stored in the database.
     */
    protected InputStream writeObject(RevObject object) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        serializer.createObjectWriter(object.getType()).write(object, bout);
        return new ByteArrayInputStream(bout.toByteArray());
    }

    /**
     * Opens a database connection, returning the object representing connection state.
     */
    protected abstract C connect(File geogitDir);

    /**
     * Closes a database connection.
     * 
     * @param cx The connection object.
     */
    protected abstract void close(C cx);

    /**
     * Creates the object table with the following schema:
     * 
     * <pre>
     * objects(id:varchar PRIMARY KEY, object:blob)
     * </pre>
     * 
     * Implementations of this method should be prepared to be called multiple times, so must check
     * if the table already exists.
     * 
     * @param cx The connection object.
     */
    protected abstract void init(C cx);

    /**
     * Determines if the object with the specified id exists.
     */
    protected abstract boolean has(String id, C cx);

    /**
     * Searches for objects with ids that match the speciifed partial string.
     * 
     * @param partialId The partial id.
     * 
     * @return Iterable of matches.
     */
    protected abstract Iterable<String> search(String partialId, C cx);

    /**
     * Retrieves the object with the specified id.
     * <p>
     * Must return <code>null</code> if no such object exists.
     * </p>
     */
    protected abstract InputStream get(String id, C cx);

    /**
     * Inserts or updates the object with the specified id.
     */
    protected abstract void put(String id, InputStream obj, C cx);

    /**
     * Deletes the object with the specified id.
     * 
     * @return Flag indicating if object was actually removed.
     */
    protected abstract boolean delete(String id, C cx);
}
>>>>>>> .merge_file_ytHIH9
