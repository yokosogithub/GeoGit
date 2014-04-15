/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage;

import org.geogit.api.ObjectId;
import org.geogit.repository.RepositoryConnectionException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class SynchronizedGraphDatabase implements GraphDatabase {
    private final GraphDatabase delegate;
    public SynchronizedGraphDatabase(GraphDatabase delegate) {
        this.delegate = delegate;
    }

    public void open() {
        synchronized(delegate) {
            delegate.open();
        }
    }

    public void configure() throws RepositoryConnectionException {
        synchronized(delegate) {
            delegate.configure();
        }
    }

    public void checkConfig() throws RepositoryConnectionException {
        synchronized(delegate) {
            delegate.checkConfig();
        }
    }

    public boolean isOpen() {
        synchronized(delegate) {
            return delegate.isOpen();
        }
    }

    public void close() {
        synchronized(delegate) {
            delegate.close();
        }
    }

    public boolean exists(final ObjectId commitId) {
        synchronized(delegate) {
            return delegate.exists(commitId);
        }
    }

    public ImmutableList<ObjectId> getParents(ObjectId commitId) throws IllegalArgumentException {
        synchronized(delegate) {
            return delegate.getParents(commitId);
        }
    }

    public ImmutableList<ObjectId> getChildren(ObjectId commitId) throws IllegalArgumentException {
        synchronized(delegate) {
            return delegate.getChildren(commitId);
        }
    }

    public boolean put(ObjectId commitId, ImmutableList<ObjectId> parentIds) {
        synchronized(delegate) {
            return delegate.put(commitId, parentIds);
        }
    }

    public void map(ObjectId mapped, ObjectId original) {
        synchronized(delegate) {
            delegate.map(mapped, original);
        }
    }

    public ObjectId getMapping(ObjectId commitId) {
        synchronized(delegate) {
            return delegate.getMapping(commitId);
        }
    }

    public int getDepth(final ObjectId commitId) {
        synchronized(delegate) {
            return delegate.getDepth(commitId);
        }
    }

    public Optional<ObjectId> findLowestCommonAncestor(ObjectId leftId, ObjectId rightId) {
        synchronized(delegate) {
            return delegate.findLowestCommonAncestor(leftId, rightId);
        }
    }

    public void setProperty(ObjectId commitId, String propertyName, String propertyValue) {
        synchronized(delegate) {
            delegate.setProperty(commitId, propertyName, propertyValue);
        }
    }

    public boolean isSparsePath(ObjectId start, ObjectId end) {
        synchronized(delegate) {
            return delegate.isSparsePath(start, end);
        }
    }

    public void truncate() {
        synchronized(delegate) {
            delegate.truncate();
        }
    }
}
