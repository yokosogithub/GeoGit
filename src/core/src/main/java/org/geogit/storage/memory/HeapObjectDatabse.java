<<<<<<< .merge_file_LSsj5P
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.BulkOpListener;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerializingFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.ning.compress.lzf.LZFInputStream;

/**
 * Provides an implementation of a GeoGit object database that utilizes the heap for the storage of
 * objects.
 * 
 * @see AbstractObjectDatabase
 */
public class HeapObjectDatabse extends AbstractObjectDatabase implements ObjectDatabase {

    private ConcurrentMap<ObjectId, byte[]> objects;

    @Inject
    public HeapObjectDatabse(final ObjectSerializingFactory sfac) {
        super(sfac);
    }

    /**
     * Closes the database.
     * 
     * @see org.geogit.storage.ObjectDatabase#close()
     */
    @Override
    public void close() {
        if (objects != null) {
            objects.clear();
            objects = null;
        }
    }

    /**
     * @return true if the database is open, false otherwise
     */
    @Override
    public boolean isOpen() {
        return objects != null;
    }

    /**
     * Opens the database for use by GeoGit.
     */
    @Override
    public void open() {
        if (isOpen()) {
            return;
        }
        objects = Maps.newConcurrentMap();
    }

    /**
     * Determines if the given {@link ObjectId} exists in the object database.
     * 
     * @param id the id to search for
     * @return true if the object exists, false otherwise
     */
    @Override
    public boolean exists(ObjectId id) {
        checkNotNull(id);
        return objects.containsKey(id);
    }

    /**
     * Deletes the object with the provided {@link ObjectId id} from the database.
     * 
     * @param objectId the id of the object to delete
     * @return true if the object was deleted, false if it was not found
     */
    @Override
    public boolean delete(ObjectId objectId) {
        checkNotNull(objectId);
        return objects.remove(objectId) != null;
    }

    @Override
    protected List<ObjectId> lookUpInternal(byte[] raw) {
        throw new UnsupportedOperationException("we override lookup directly");
    }

    /**
     * Searches the database for {@link ObjectId}s that match the given partial id.
     * 
     * @param partialId the partial id to search for
     * @return a list of matching results
     */
    @Override
    public List<ObjectId> lookUp(final String partialId) {
        Preconditions.checkNotNull(partialId);
        List<ObjectId> matches = Lists.newLinkedList();
        for (ObjectId id : objects.keySet()) {
            if (id.toString().startsWith(partialId)) {
                matches.add(id);
            }
        }
        return matches;
    }

    @Override
    protected InputStream getRawInternal(ObjectId id, boolean failIfNotFound)
            throws IllegalArgumentException {
        byte[] data = objects.get(id);
        if (data == null) {
            if (failIfNotFound) {
                throw new IllegalArgumentException(id + " does not exist");
            }
            return null;
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    protected boolean putInternal(ObjectId id, byte[] rawData) {
        byte[] previousValue = objects.putIfAbsent(id, rawData);
        return previousValue == null;
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, final BulkOpListener listener) {
        long count = 0;
        while (ids.hasNext()) {
            ObjectId id = ids.next();
            byte[] removed = this.objects.remove(id);
            if (removed != null) {
                count++;
                listener.deleted(id);
            } else {
                listener.notFound(id);
            }
        }
        return count;
    }

    @Override
    public Iterator<RevObject> getAll(final Iterable<ObjectId> ids, final BulkOpListener listener) {

        return new AbstractIterator<RevObject>() {
            final Iterator<ObjectId> iterator = ids.iterator();

            @Override
            protected RevObject computeNext() {
                RevObject found = null;
                ObjectId id;
                byte[] raw;
                while (iterator.hasNext() && found == null) {
                    id = iterator.next();
                    raw = objects.get(id);
                    if (raw != null) {
                        try {
                            found = serializationFactory.createObjectReader().read(id,
                                    new LZFInputStream(new ByteArrayInputStream(raw)));
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                        listener.found(found.getId(), raw.length);
                    } else {
                        listener.notFound(id);
                    }
                }
                return found == null ? endOfData() : found;
            }
        };
    }

    @Override
    public void configure() {
        // No-op
    }

    @Override
    public void checkConfig() {
        // No-op
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.BulkOpListener;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.datastream.DataStreamSerializationFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ning.compress.lzf.LZFInputStream;

/**
 * Provides an implementation of a GeoGit object database that utilizes the heap for the storage of
 * objects.
 * 
 * @see AbstractObjectDatabase
 */
public class HeapObjectDatabse extends AbstractObjectDatabase implements ObjectDatabase {

    private ConcurrentMap<ObjectId, byte[]> objects;

    public HeapObjectDatabse() {
        super(DataStreamSerializationFactory.INSTANCE);
    }

    /**
     * Closes the database.
     * 
     * @see org.geogit.storage.ObjectDatabase#close()
     */
    @Override
    public void close() {
        if (objects != null) {
            objects.clear();
            objects = null;
        }
    }

    /**
     * @return true if the database is open, false otherwise
     */
    @Override
    public boolean isOpen() {
        return objects != null;
    }

    /**
     * Opens the database for use by GeoGit.
     */
    @Override
    public void open() {
        if (isOpen()) {
            return;
        }
        objects = Maps.newConcurrentMap();
    }

    /**
     * Determines if the given {@link ObjectId} exists in the object database.
     * 
     * @param id the id to search for
     * @return true if the object exists, false otherwise
     */
    @Override
    public boolean exists(ObjectId id) {
        checkNotNull(id);
        return objects.containsKey(id);
    }

    /**
     * Deletes the object with the provided {@link ObjectId id} from the database.
     * 
     * @param objectId the id of the object to delete
     * @return true if the object was deleted, false if it was not found
     */
    @Override
    public boolean delete(ObjectId objectId) {
        checkNotNull(objectId);
        return objects.remove(objectId) != null;
    }

    @Override
    protected List<ObjectId> lookUpInternal(byte[] raw) {
        throw new UnsupportedOperationException("we override lookup directly");
    }

    /**
     * Searches the database for {@link ObjectId}s that match the given partial id.
     * 
     * @param partialId the partial id to search for
     * @return a list of matching results
     */
    @Override
    public List<ObjectId> lookUp(final String partialId) {
        Preconditions.checkNotNull(partialId);
        List<ObjectId> matches = Lists.newLinkedList();
        for (ObjectId id : objects.keySet()) {
            if (id.toString().startsWith(partialId)) {
                matches.add(id);
            }
        }
        return matches;
    }

    @Override
    protected InputStream getRawInternal(ObjectId id, boolean failIfNotFound)
            throws IllegalArgumentException {
        byte[] data = objects.get(id);
        if (data == null) {
            if (failIfNotFound) {
                throw new IllegalArgumentException(id + " does not exist");
            }
            return null;
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    protected boolean putInternal(ObjectId id, byte[] rawData) {
        byte[] previousValue = objects.putIfAbsent(id, rawData);
        return previousValue == null;
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, final BulkOpListener listener) {
        long count = 0;
        while (ids.hasNext()) {
            ObjectId id = ids.next();
            byte[] removed = this.objects.remove(id);
            if (removed != null) {
                count++;
                listener.deleted(id);
            } else {
                listener.notFound(id);
            }
        }
        return count;
    }

    @Override
    public Iterator<RevObject> getAll(final Iterable<ObjectId> ids, final BulkOpListener listener) {

        return new AbstractIterator<RevObject>() {
            final Iterator<ObjectId> iterator = ids.iterator();

            @Override
            protected RevObject computeNext() {
                RevObject found = null;
                ObjectId id;
                byte[] raw;
                while (iterator.hasNext() && found == null) {
                    id = iterator.next();
                    raw = objects.get(id);
                    if (raw != null) {
                        try {
                            found = serializationFactory.createObjectReader().read(id,
                                    new LZFInputStream(new ByteArrayInputStream(raw)));
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                        listener.found(found.getId(), raw.length);
                    } else {
                        listener.notFound(id);
                    }
                }
                return found == null ? endOfData() : found;
            }
        };
    }

    @Override
    public void configure() {
        // No-op
    }

    @Override
    public void checkConfig() {
        // No-op
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
>>>>>>> .merge_file_yy9eZP
