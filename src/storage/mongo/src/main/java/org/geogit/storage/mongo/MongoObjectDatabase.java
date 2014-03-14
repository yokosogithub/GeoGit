/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.mongo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogit.api.ObjectId;
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
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.datastream.DataStreamSerializationFactory;

import com.google.common.base.Functions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

/**
 * An Object database that uses a MongoDB server for persistence.
 * 
 * @see http://mongodb.com/
 */
public class MongoObjectDatabase implements ObjectDatabase {
    private final MongoConnectionManager manager;

    protected final ConfigDatabase config;

    private MongoClient client = null;

    protected DB db = null;

    protected DBCollection collection = null;

    protected ObjectSerializingFactory serializers = new DataStreamSerializationFactory();

    private String collectionName;

    @Inject
    public MongoObjectDatabase(ConfigDatabase config, MongoConnectionManager manager) {
        this(config, manager, "objects");
    }

    MongoObjectDatabase(ConfigDatabase config, MongoConnectionManager manager, String collectionName) {
        this.config = config;
        this.manager = manager;
        this.collectionName = collectionName;
    }

    private RevObject fromBytes(ObjectId id, byte[] buffer) {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer);
        RevObject result = serializers.createObjectReader().read(id, byteStream);
        return result;
    }

    private byte[] toBytes(RevObject object) {
        ObjectWriter<RevObject> writer = serializers.createObjectWriter(object.getType());
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            writer.write(object, byteStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteStream.toByteArray();
    }

    protected String getCollectionName() {
        return collectionName;
    }

    @Override
    public synchronized void open() {
        if (client != null) {
            return;
        }
        String uri = config.get("mongodb.uri").get();
        String database = config.get("mongodb.database").get();
        client = manager.acquire(new MongoAddress(uri));
        db = client.getDB(database);
        collection = db.getCollection(getCollectionName());
        collection.ensureIndex("oid");
    }

    @Override
    public synchronized boolean isOpen() {
        return client != null;
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.configure(config, "mongodb", "0.1");
        String uri = config.get("mongodb.uri").or(config.getGlobal("mongodb.uri"))
                .or("mongodb://localhost:27017/");
        String database = config.get("mongodb.database").or(config.getGlobal("mongodb.database"))
                .or("geogit");
        config.put("mongodb.uri", uri);
        config.put("mongodb.database", database);
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.verify(config, "mongodb", "0.1");
    }

    @Override
    public synchronized void close() {
        if (client != null) {
            manager.release(client);
        }
        client = null;
        db = null;
        collection = null;
    }

    @Override
    public boolean exists(ObjectId id) {
        DBObject query = new BasicDBObject();
        query.put("oid", id.toString());
        return collection.find(query).hasNext();
    }

    @Override
    public List<ObjectId> lookUp(final String partialId) {
        if (partialId.matches("[a-fA-F0-9]+")) {
            DBObject regex = new BasicDBObject();
            regex.put("$regex", "^" + partialId);
            DBObject query = new BasicDBObject();
            query.put("oid", regex);
            DBCursor cursor = collection.find(query);
            List<ObjectId> ids = new ArrayList<ObjectId>();
            while (cursor.hasNext()) {
                DBObject elem = cursor.next();
                String oid = (String) elem.get("oid");
                ids.add(ObjectId.valueOf(oid));
            }
            return ids;
        } else {
            throw new IllegalArgumentException(
                    "Prefix query must be done with hexadecimal values only");
        }
    }

    @Override
    public RevObject get(ObjectId id) {
        RevObject result = getIfPresent(id);
        if (result != null) {
            return result;
        } else {
            throw new NoSuchElementException("No object with id: " + id);
        }
    }

    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> clazz) {
        return clazz.cast(get(id));
    }

    @Override
    public RevObject getIfPresent(ObjectId id) {
        DBObject query = new BasicDBObject();
        query.put("oid", id.toString());
        DBCursor results = collection.find(query);
        if (results.hasNext()) {
            DBObject result = results.next();
            return fromBytes(id, (byte[]) result.get("serialized_object"));
        } else {
            return null;
        }
    }

    @Override
    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> clazz) {
        return clazz.cast(getIfPresent(id));
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

    private long deleteChunk(List<ObjectId> ids) {
        List<String> idStrings = Lists.transform(ids, Functions.toStringFunction());
        DBObject query = BasicDBObjectBuilder.start().push("oid").add("$in", idStrings).pop().get();
        WriteResult result = collection.remove(query);
        return result.getN();
    }

    @Override
    public boolean delete(ObjectId id) {
        DBObject query = new BasicDBObject();
        query.put("oid", id.toString());
        return collection.remove(query).getLastError().ok();
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids) {
        return deleteAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, BulkOpListener listener) {
        Iterator<List<ObjectId>> chunks = Iterators.partition(ids, 500);
        long count = 0;
        while (chunks.hasNext()) {
            count += deleteChunk(chunks.next());
        }
        return count;
    }

    @Override
    public boolean put(final RevObject object) {
        DBObject query = new BasicDBObject();
        query.put("oid", object.getId().toString());
        DBObject record = new BasicDBObject();
        record.put("oid", object.getId().toString());
        record.put("serialized_object", toBytes(object));
        return collection.update(query, record, true, false).getLastError().ok();
    }

    @Override
    public void putAll(final Iterator<? extends RevObject> objects) {
        putAll(objects, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects, BulkOpListener listener) {
        while (objects.hasNext()) {
            RevObject object = objects.next();
            boolean put = put(object);
            if (put) {
                listener.inserted(object.getId(), null);
            } else {
                listener.found(object.getId(), null);
            }
        }
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return new ObjectInserter(this);
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids) {
        return getAll(ids, BulkOpListener.NOOP_LISTENER);
    }

    @Override
    public Iterator<RevObject> getAll(final Iterable<ObjectId> ids, final BulkOpListener listener) {

        return new AbstractIterator<RevObject>() {
            final Iterator<ObjectId> queryIds = ids.iterator();

            @Override
            protected RevObject computeNext() {
                RevObject obj = null;
                while (obj == null) {
                    if (!queryIds.hasNext()) {
                        return endOfData();
                    }
                    ObjectId id = queryIds.next();
                    obj = getIfPresent(id);
                    if (obj == null) {
                        listener.notFound(id);
                    } else {
                        listener.found(obj.getId(), null);
                    }
                }
                return obj == null ? endOfData() : obj;
            }
        };
    }

    public DBCollection getCollection(String name) {
        return db.getCollection(name);
    }

    @Override
    public String toString() {
        return String.format("%s[db: %s, collection: %s]", getClass().getSimpleName(),
                db == null ? "<unset>" : db, collectionName);
    }
}
