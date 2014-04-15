/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import static com.google.common.base.Suppliers.ofInstance;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ForwardingStagingDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerializingFactory;

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
public class HeapStagingDatabase extends ForwardingStagingDatabase {
    private Map<String, Map<String, Conflict>> conflicts = Maps.newHashMap();

    /**
     * @param repositoryDb the repository reference database, used to get delegate read operations
     *        to for objects not found here
     */
    @Inject
    public HeapStagingDatabase(final ObjectDatabase repositoryDb,
            final ObjectSerializingFactory serialFactory) {
        super(ofInstance(repositoryDb), ofInstance(new HeapObjectDatabse(serialFactory)));
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        // No-op
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        // No-op
    }

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
