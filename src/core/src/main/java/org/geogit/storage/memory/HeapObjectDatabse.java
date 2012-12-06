package org.geogit.storage.memory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Provides an implementation of a GeoGit object database that utilizes the heap for the storage of
 * objects.
 * 
 * @see AbstractObjectDatabase
 */
public class HeapObjectDatabse extends AbstractObjectDatabase implements ObjectDatabase {

    private Map<ObjectId, byte[]> objects;

    @Inject
    public HeapObjectDatabse(final ObjectSerialisingFactory sfac) {
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
        Map<ObjectId, byte[]> map = Maps.newTreeMap();
        objects = Collections.synchronizedMap(map);
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
    protected InputStream getRawInternal(ObjectId id) throws IllegalArgumentException {
        byte[] data = objects.get(id);
        if (data == null) {
            throw new IllegalArgumentException(id + " does not exist");
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    protected boolean putInternal(ObjectId id, byte[] rawData) {
        if (exists(id)) {
            return false;
        }
        objects.put(id, rawData);
        return true;
    }

}
