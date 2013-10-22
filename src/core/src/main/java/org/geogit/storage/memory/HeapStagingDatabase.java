/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.google.inject.Inject;

/**
 * Provides an implementation of a GeoGit staging database that utilizes the heap for the storage of
 * objects.
 * 
 * @see AbstractObjectDatabase
 */
public class HeapStagingDatabase extends HeapObjectDatabse implements StagingDatabase {

    private ObjectDatabase repositoryDb;

    /**
     * @param repositoryDb the repository reference database, used to get delegate read operations
     *        to for objects not found here
     */
    @Inject
    public HeapStagingDatabase(final ObjectDatabase repositoryDb,
            final ObjectSerializingFactory serialFactory) {
        super(serialFactory);
        this.repositoryDb = repositoryDb;
    }

    // /////////////////////////////////////////

    @Override
    public boolean exists(ObjectId id) {
        boolean exists = super.exists(id);
        if (!exists) {
            exists = repositoryDb.exists(id);
        }
        return exists;
    }

    @Override
    protected InputStream getRawInternal(final ObjectId id, final boolean failIfNotFound)
            throws IllegalArgumentException {

        return super.getRawInternal(id, failIfNotFound);
    }

    /**
     * Searches the database for {@link ObjectId}s that match the given partial id.
     * 
     * @param partialId the partial id to search for
     * @return a list of matching results
     */
    @Override
    public List<ObjectId> lookUp(String partialId) {
        Set<ObjectId> lookUp = new HashSet<ObjectId>(super.lookUp(partialId));
        lookUp.addAll(repositoryDb.lookUp(partialId));
        return new ArrayList<ObjectId>(lookUp);
    }

    /**
     * Reads an object with the given {@link ObjectId id} out of the database.
     * 
     * @param id the id of the object to read
     * @param reader the reader of the object
     * @return the object, as read in from the {@link ObjectReader}
     */
    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> type) {
        T obj = super.getIfPresent(id, type);
        if (null == obj) {
            obj = repositoryDb.get(id, type);
        }
        return obj;
    }

    @Override
    @Nullable
    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> clazz)
            throws IllegalArgumentException {
        T obj = super.getIfPresent(id, clazz);
        if (null == obj) {
            obj = repositoryDb.getIfPresent(id, clazz);
        }
        return obj;
    }

    @Override
    public RevObject get(ObjectId id) {
        RevObject obj = super.getIfPresent(id);
        if (null == obj) {
            obj = repositoryDb.get(id);
        }
        return obj;
    }

    @Override
    public @Nullable
    RevObject getIfPresent(ObjectId id) {
        RevObject obj = super.getIfPresent(id);
        if (null == obj) {
            obj = repositoryDb.getIfPresent(id);
        }
        return obj;
    }

    /**
     * @return a newly constructed {@link ObjectInserter} for this database
     */
    @Override
    public ObjectInserter newObjectInserter() {
        return super.newObjectInserter();
    }

    /**
     * Deletes the object with the provided {@link ObjectId id} from the database.
     * 
     * @param objectId the id of the object to delete
     * @return true if the object was deleted, false if it was not found
     */
    @Override
    public boolean delete(ObjectId objectId) {
        return super.delete(objectId);
    }

    private Map<String, Map<String, Conflict>> conflicts = Maps.newHashMap();

    /**
     * Gets all conflicts that match the specified path filter.
     * 
     * @param namespace the namespace of the conflict
     * @param pathFilter the path filter, if this is not defined, all conflicts will be returned
     * @return the list of conflicts
     */
    @Override
    public List<Conflict> getConflicts(@Nullable String namespace, @Nullable final String pathFilter) {
        if (namespace == null) {
            namespace = "root";
        }

        if (conflicts.get(namespace) == null) {
            return ImmutableList.of();
        }
        if (pathFilter == null) {
            return ImmutableList.copyOf(conflicts.get(namespace).values());
        }
        UnmodifiableIterator<Conflict> filtered = Iterators.filter(conflicts.get(namespace)
                .values().iterator(), new Predicate<Conflict>() {
            @Override
            public boolean apply(@Nullable Conflict c) {
                return (c.getPath().startsWith(pathFilter));
            }

        });
        return ImmutableList.copyOf(filtered);
    }

    /**
     * Adds a conflict to the database.
     * 
     * @param namespace the namespace of the conflict
     * @param conflict the conflict to add
     */
    @Override
    public void addConflict(@Nullable String namespace, Conflict conflict) {
        if (namespace == null) {
            namespace = "root";
        }
        Map<String, Conflict> conflictMap = conflicts.get(namespace);
        if (conflictMap == null) {
            conflictMap = Maps.newHashMap();
            conflicts.put(namespace, conflictMap);
        }
        conflictMap.put(conflict.getPath(), conflict);

    }

    /**
     * Removes a conflict from the database.
     * 
     * @param namespace the namespace of the conflict
     * @param path the path of feature whose conflict should be removed
     */
    @Override
    public void removeConflict(@Nullable String namespace, String path) {
        if (namespace == null) {
            namespace = "root";
        }
        Map<String, Conflict> conflictMap = conflicts.get(namespace);
        if (conflictMap != null) {
            conflictMap.remove(path);
        }
    }

    /**
     * Gets the specified conflict from the database.
     * 
     * @param namespace the namespace of the conflict
     * @param path the conflict to retrieve
     * @return the conflict, or {@link Optional#absent()} if it was not found
     */
    @Override
    public Optional<Conflict> getConflict(@Nullable String namespace, String path) {
        if (namespace == null) {
            namespace = "root";
        }
        Map<String, Conflict> conflictMap = conflicts.get(namespace);
        if (conflictMap != null) {
            return Optional.fromNullable(conflictMap.get(path));
        }
        return Optional.absent();
    }

    /**
     * Removes all conflicts from the database.
     * 
     * @param namespace the namespace of the conflicts to remove
     */
    @Override
    public void removeConflicts(@Nullable String namespace) {
        if (namespace == null) {
            namespace = "root";
        }
        conflicts.remove(namespace);
    }

}
