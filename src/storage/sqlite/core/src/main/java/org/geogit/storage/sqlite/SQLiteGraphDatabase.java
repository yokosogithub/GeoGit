/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import static org.geogit.storage.sqlite.SQLiteStorage.FORMAT_NAME;
import static org.geogit.storage.sqlite.SQLiteStorage.VERSION;

import java.io.File;
import java.util.List;
import java.util.Queue;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Base class for SQLite based graph database.
 * 
 * @author Justin Deoliveira, Boundless
 * 
 * @param <C> Connection type.
 */
public abstract class SQLiteGraphDatabase<T> implements GraphDatabase {

    final ConfigDatabase configdb;

    final Platform platform;

    private T cx;

    public SQLiteGraphDatabase(ConfigDatabase configdb, Platform platform) {
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
        RepositoryConnectionException.StorageType.GRAPH.configure(configdb, FORMAT_NAME, VERSION);
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.GRAPH.verify(configdb, FORMAT_NAME, VERSION);
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
    public boolean exists(ObjectId commitId) {
        return has(commitId.toString(), cx);
    }

    @Override
    public ImmutableList<ObjectId> getParents(ObjectId commitId) throws IllegalArgumentException {
        return ImmutableList.copyOf(Iterables.transform(outgoing(commitId.toString(), cx),
                StringToObjectId.INSTANCE));
    }

    @Override
    public ImmutableList<ObjectId> getChildren(ObjectId commitId) throws IllegalArgumentException {
        return ImmutableList.copyOf(Iterables.transform(incoming(commitId.toString(), cx),
                StringToObjectId.INSTANCE));
    }

    @Override
    public boolean put(ObjectId commitId, ImmutableList<ObjectId> parentIds) {
        String node = commitId.toString();
        boolean added = put(node, cx);

        // TODO: if node was node added should we severe existing parent relationships?
        for (ObjectId p : parentIds) {
            relate(node, p.toString(), cx);
        }
        return added;
    }

    @Override
    public void map(ObjectId mapped, ObjectId original) {
        map(mapped.toString(), original.toString(), cx);
    }

    @Override
    public ObjectId getMapping(ObjectId commitId) {
        String mapped = mapping(commitId.toString(), cx);
        return mapped != null ? ObjectId.valueOf(mapped) : null;
    }

    @Override
    public int getDepth(ObjectId commitId) {
        int depth = 0;

        Queue<String> q = Lists.newLinkedList();
        Iterables.addAll(q, outgoing(commitId.toString(), cx));

        List<String> next = Lists.newArrayList();
        while (!q.isEmpty()) {
            depth++;
            while (!q.isEmpty()) {
                String n = q.poll();
                List<String> parents = Lists.newArrayList(outgoing(n, cx));
                if (parents.size() == 0) {
                    return depth;
                }

                Iterables.addAll(next, parents);
            }

            q.addAll(next);
            next.clear();
        }

        return depth;
    }

    @Override
    public Optional<ObjectId> findLowestCommonAncestor(ObjectId leftId, ObjectId rightId) {
        PathToRootWalker<T> left = new PathToRootWalker<T>(leftId, this, cx);
        PathToRootWalker<T> right = new PathToRootWalker<T>(rightId, this, cx);

        while (left.hasNext() || right.hasNext()) {
            if (left.hasNext()) {
                for (ObjectId node : left.next()) {
                    if (right.seen(node)) {
                        return Optional.of(node);
                    }
                }
            }
            if (right.hasNext()) {
                for (ObjectId node : right.next()) {
                    if (left.seen(node)) {
                        return Optional.of(node);
                    }
                }
            }
        }

        return Optional.absent();
    }

    @Override
    public void setProperty(ObjectId commitId, String name, String value) {
        property(commitId.toString(), name, value, cx);
    }

    @Override
    public boolean isSparsePath(ObjectId start, ObjectId end) {
        ShortestPathWalker<T> p = new ShortestPathWalker<T>(start, end, this, cx);
        while (p.hasNext()) {
            ObjectId node = p.next();
            if (Boolean.valueOf(property(node.toString(), GraphDatabase.SPARSE_FLAG, cx))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void truncate() {
    }

    /**
     * Opens a database connection, returning the object representing connection state.
     */
    protected abstract T connect(File geogitDir);

    /**
     * Closes a database connection.
     * 
     * @param cx The connection object.
     */
    protected abstract void close(T cx);

    /**
     * Creates the graph tables with the following schema:
     * 
     * <pre>
     * nodes(id:varchar PRIMARY KEY)
     * edges(src:varchar, dst:varchar, PRIMARY KEY(src,dst))
     * props(nid:varchar, key:varchar, val:varchar, PRIMARY KEY(nid,key))
     * mappings(alias:varchar, nid:varchar)
     * </pre>
     * 
     * Implementations of this method should be prepared to be called multiple times, so must check
     * if the tables already exist.
     * 
     * @param cx The connection object.
     */
    protected abstract void init(T cx);

    /**
     * Adds a new node to the graph.
     * <p>
     * This method must determine if the node already exists in the graph.
     * </p>
     * 
     * @return True if the node did not previously exist in the graph, false if otherwise.
     */
    protected abstract boolean put(String node, T cx);

    /**
     * Determines if a node exists in the graph.
     */
    protected abstract boolean has(String node, T cx);

    /**
     * Relates two nodes in the graph.
     * 
     * @param src The source (origin) node of the relationship.
     * @param dst The destination (origin) node of the relationship.
     */
    protected abstract void relate(String src, String dst, T cx);

    /**
     * Creates a node mapping.
     * 
     * @param from The node being mapped from.
     * @param to The node being mapped to.
     */
    protected abstract void map(String from, String to, T cx);

    /**
     * Returns the mapping for a node.
     * <p>
     * This method should return <code>null</code> if no mapping exists.
     * </p>
     */
    protected abstract String mapping(String node, T cx);

    /**
     * Assigns a property key/value pair to a node.
     * 
     * @param node The node.
     * @param key The property key.
     * @param value The property value.
     */
    protected abstract void property(String node, String key, String value, T cx);

    /**
     * Retrieves a property by key from a node.
     * 
     * @param node The node.
     * @param key The property key.
     * 
     * @return The property value, or <code>null</code> if the property is not set for the node.
     */
    protected abstract String property(String node, String key, T cx);

    /**
     * Returns all nodes connected to the specified node through a relationship in which the
     * specified node is the "source" of the relationship.
     */
    protected abstract Iterable<String> outgoing(String node, T cx);

    /**
     * Returns all nodes connected to the specified node through a relationship in which the
     * specified node is the "destination" of the relationship.
     */
    protected abstract Iterable<String> incoming(String node, T cx);

    /**
     * Clears the contents of the graph.
     */
    protected abstract void clear(T cx);
}
