/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.mongo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.StagingDatabase;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * @TODO: extract interface
 */
public class MongoStagingDatabase extends MongoObjectDatabase implements
        StagingDatabase {
    private ObjectSerializingFactory sfac;
    private ObjectDatabase repositoryDb;
    protected DBCollection conflicts;

    protected String getCollectionName() {
        return "staging";
    }

    @Inject
    public MongoStagingDatabase(final ConfigDatabase config,
            final MongoConnectionManager manager,
            final ObjectSerializingFactory sfac,
            final ObjectDatabase repositoryDb) {
        super(config, manager);
        this.sfac = sfac;
        this.repositoryDb = repositoryDb;
    }

    @Override
    synchronized public void open() {
        super.open();
        conflicts = db.getCollection("conflicts");
        conflicts.ensureIndex("path");
    }

    @Override
    synchronized public void close() {
        super.close();
        conflicts = null;
    }

    @Override
    public boolean exists(ObjectId id) {
        return super.exists(id) || repositoryDb.exists(id);
    }

    @Override
    public InputStream getRaw(ObjectId id) {
        try {
            return super.getRaw(id);
        } catch (NoSuchElementException e) {
            return repositoryDb.getRaw(id);
        }
    }

    @Override
    public List<ObjectId> lookUp(final String partialId) {
        Set<ObjectId> results = new HashSet<ObjectId>();
        // Using a set because we were getting duplicates building a list
        // directly. Need to figure out why we are ending up with objects in
        // both staging and object database... Maybe delete() is not working
        // properly?
        results.addAll(super.lookUp(partialId));
        results.addAll(repositoryDb.lookUp(partialId));
        return new ArrayList(results);
    }

    @Override
    public RevObject getIfPresent(ObjectId id) {
        RevObject result = super.getIfPresent(id);
        if (result != null) {
            return result;
        } else {
            return repositoryDb.getIfPresent(id);
        }
    }

    public Optional<Conflict> getConflict(@Nullable String namespace,
            String path) {
        DBObject query = new BasicDBObject();
        query.put("path", path);
        if (namespace != null) {
            query.put("namespace", namespace);
        }
        DBObject result = conflicts.findOne(query);
        if (result == null) {
            return Optional.absent();
        } else {
            ObjectId ancestor = ObjectId.valueOf((String) result
                    .get("ancestor"));
            ObjectId ours = ObjectId.valueOf((String) result.get("ours"));
            ObjectId theirs = ObjectId.valueOf((String) result.get("theirs"));
            return Optional.of(new Conflict(path, ancestor, ours, theirs));
        }
    }

    public List<Conflict> getConflicts(@Nullable String namespace,
            @Nullable String pathFilter) {
        DBObject query = new BasicDBObject();
        if (namespace == null) {
            query.put("namespace", 0);
        } else {
            query.put("namespace", namespace);
        }
        if (pathFilter != null) {
            DBObject regex = new BasicDBObject();
            regex.put("$regex", "^" + pathFilter);
            query.put("path", regex);
        }
        DBCursor cursor = conflicts.find(query);
        List<Conflict> results = new ArrayList<Conflict>();
        while (cursor.hasNext()) {
            DBObject element = cursor.next();
            String path = (String) element.get("path");
            ObjectId ancestor = ObjectId.valueOf((String) element
                    .get("ancestor"));
            ObjectId ours = ObjectId.valueOf((String) element.get("ours"));
            ObjectId theirs = ObjectId.valueOf((String) element.get("theirs"));
            results.add(new Conflict(path, ancestor, ours, theirs));
        }
        return results;
    }

    public void addConflict(@Nullable String namespace, Conflict conflict) {
        DBObject query = new BasicDBObject();
        query.put("path", conflict.getPath());
        if (namespace == null) {
            query.put("namespace", 0);
        } else {
            query.put("namespace", namespace);
        }
        DBObject record = new BasicDBObject();
        if (namespace == null) {
            record.put("namespace", 0);
        } else {
            record.put("namespace", namespace);
        }
        record.put("path", conflict.getPath());
        record.put("ancestor", conflict.getAncestor().toString());
        record.put("ours", conflict.getOurs().toString());
        record.put("theirs", conflict.getTheirs().toString());
        conflicts.update(query, record, true, false);
    }

    public void removeConflict(@Nullable String namespace, String path) {
        DBObject query = new BasicDBObject();
        if (namespace == null) {
            query.put("namespace", 0);
        } else {
            query.put("namespace", namespace);
        }
        query.put("path", path);
        conflicts.remove(query);
    }

    public void removeConflicts(@Nullable String namespace) {
        DBObject query = new BasicDBObject();
        if (namespace == null) {
            query.put("namespace", 0);
        } else {
            query.put("namespace", namespace);
        }
        conflicts.remove(query);
    }
}
