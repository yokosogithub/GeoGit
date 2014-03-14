/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static org.geogit.storage.sqlite.SQLiteStorage.FORMAT_NAME;
import static org.geogit.storage.sqlite.SQLiteStorage.VERSION;

import java.util.List;

import org.geogit.api.Platform;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ForwardingStagingDatabase;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Base class for SQLite based staging database.
 * 
 * @author Justin Deoliveira, Boundless
 * 
 * @param <T>
 */
public abstract class SQLiteStagingDatabase<T> extends ForwardingStagingDatabase {

    final ConfigDatabase configdb;

    final Platform platform;

    private T cx;

    public SQLiteStagingDatabase(ObjectDatabase repoDb, SQLiteObjectDatabase<T> stageDb,
            ConfigDatabase configdb, Platform platform) {

        super(Suppliers.ofInstance(repoDb), Suppliers.ofInstance(stageDb));

        this.configdb = configdb;
        this.platform = platform;
    }

    @Override
    public void open() {
        super.open();

        cx = ((SQLiteObjectDatabase<T>) stagingDb).cx;
        init(cx);
    }

    @Override
    public Optional<Conflict> getConflict(String namespace, String path) {
        List<Conflict> conflicts = getConflicts(namespace, path);
        if (conflicts.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(conflicts.get(0));
    }

    @Override
    public List<Conflict> getConflicts(String namespace, String pathFilter) {
        return Lists.newArrayList(Iterables.transform(get(namespace, pathFilter, cx),
                StringToConflict.INSTANCE));
    }

    @Override
    public void addConflict(String namespace, Conflict conflict) {
        put(namespace, conflict.getPath(), conflict.toString(), cx);
    }

    @Override
    public void removeConflict(String namespace, String path) {
        remove(namespace, path, cx);
    }

    @Override
    public void removeConflicts(String namespace) {
        for (Conflict c : Iterables.transform(get(namespace, null, cx), StringToConflict.INSTANCE)) {
            removeConflict(namespace, c.getPath());
        }
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.STAGING.configure(configdb, FORMAT_NAME, VERSION);
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.STAGING.verify(configdb, FORMAT_NAME, VERSION);
    }

    /**
     * Creates the object table with the following schema:
     * 
     * <pre>
     * conflicts(namespace:varchar, path:varchar, conflict:varchar)
     * </pre>
     * 
     * Implementations of this method should be prepared to be called multiple times, so must check
     * if the table already exists.
     * 
     * @param cx The connection object.
     */
    protected abstract void init(T cx);

    /**
     * Returns all conflicts matching the specified namespace and pathFilter.
     * 
     * @param namespace Namespace value, may be <code>null</code>.
     * @param pathFilter Path filter, may be <code>null</code>.
     * 
     */
    protected abstract Iterable<String> get(String namespace, String pathFilter, T cx);

    /**
     * Adds a conflict.
     * 
     * @param namespace The conflict namespace.
     * @param path The path of the conflict.
     * @param conflict The conflict value.
     */
    protected abstract void put(String namespace, String path, String conflict, T cx);

    /**
     * Removed a conflict.
     * 
     * @param namespace The conflict namespace.
     * @param path The path of the conflict.
     */
    protected abstract void remove(String namespace, String path, T cx);
}
