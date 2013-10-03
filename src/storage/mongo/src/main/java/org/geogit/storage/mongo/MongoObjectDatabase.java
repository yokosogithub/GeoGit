/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.mongo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.datastream.DataStreamSerializationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

public class MongoObjectDatabase implements ObjectDatabase {
    private MongoConnectionManager manager;

    private MongoClient client = null;
    protected DB db = null;
    protected DBCollection collection = null;
    protected ObjectSerializingFactory serializers = new DataStreamSerializationFactory();

    @Inject
    public MongoObjectDatabase(MongoConnectionManager manager) {
        this.manager = manager;
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
        return "objects";
    }

    public synchronized void open() { 
        if (client != null) {
            return;
        }
        client = manager.acquire(new MongoAddress("192.168.122.165", 27017));
        db = client.getDB("geogit");
        collection = db.getCollection(getCollectionName());
        collection.ensureIndex("oid");
    }

    public synchronized boolean isOpen() {
        return client != null;
    }
    
    public synchronized void close() { 
        if (client != null) {
            manager.release(client);
        }
        client = null;
        db = null;
        collection = null;
    }

    public boolean exists(ObjectId id) { 
        DBObject query = new BasicDBObject();
        query.put("oid", id.toString());
        return collection.find(query).hasNext();
    }

    public InputStream getRaw(ObjectId id) {
        DBObject query = new BasicDBObject();
        query.put("oid", id.toString());
        DBObject result = collection.findOne(query);
        if (result == null) {
            throw new NoSuchElementException("No object found for raw query");
        }
        byte[] bytes = (byte[]) result.get("serialized_object");
        return new ByteArrayInputStream(bytes);
    }

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
            throw new IllegalArgumentException("Prefix query must be done with hexadecimal values only");
        }
    }

    public RevObject get(ObjectId id) {
        RevObject result = getIfPresent(id);
        if (result != null) {
            return result;
        } else {
            throw new NoSuchElementException("No object with id: " + id);
        }
    }

    public <T extends RevObject> T get(ObjectId id, Class<T> clazz) {
        return clazz.cast(get(id));
    }

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

    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> clazz) { 
        return clazz.cast(getIfPresent(id));
    }

    public RevTree getTree(ObjectId id) {
        return getIfPresent(id, RevTree.class);
    }

    public RevFeature getFeature(ObjectId id) {
        return getIfPresent(id, RevFeature.class);
    }

    public RevFeatureType getFeatureType(ObjectId id) {
        return getIfPresent(id, RevFeatureType.class);
    }

    public RevCommit getCommit(ObjectId id) {
        return getIfPresent(id, RevCommit.class);
    }

    public RevTag getTag(ObjectId id) {
        return getIfPresent(id, RevTag.class);
    }

    public boolean delete(ObjectId id) {
        DBObject query = new BasicDBObject();
        query.put("oid", id.toString());
        return collection.remove(query).getLastError().ok();
    }

    public long deleteAll(Iterator<ObjectId> ids) { 
        long count = 0;
        while (ids.hasNext()) {
            if (delete(ids.next())) count += 1;
        }
        return count;
    }

    public boolean put(final RevObject object) { 
        DBObject query = new BasicDBObject();
        query.put("oid", object.getId().toString());
        DBObject record = new BasicDBObject();
        record.put("oid", object.getId().toString());
        record.put("serialized_object", toBytes(object));
        return collection.update(query, record, true, false).getLastError().ok();
    }

    public boolean put(ObjectId objectId, InputStream raw) {
        ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try {
            byte[] buff = new byte[4096];
            int len;
            while ((len = (raw.read(buff))) >= 0) {
                bytes.write(buff, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DBObject query = new BasicDBObject();
        query.put("oid", objectId.toString());
        DBObject record = new BasicDBObject();
        record.put("oid", objectId.toString());
        record.put("serialized_object", bytes.toByteArray());
        return collection.update(query, record, true, false).getLastError().ok();
    }

    public void putAll(final Iterator<? extends RevObject> objects) {
        while (objects.hasNext()) put(objects.next());
    }

    public ObjectInserter newObjectInserter() { 
        return new ObjectInserter(this);
    }
}

