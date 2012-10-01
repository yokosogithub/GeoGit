package org.geogit.storage.memory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 *
 */
public class HeapObjectDatabse extends AbstractObjectDatabase implements ObjectDatabase {

    private Map<ObjectId, byte[]> objects;

    /**
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

    @Override
    public boolean isOpen() {
        return objects != null;
    }

    /**
     * 
     * @see org.geogit.storage.ObjectDatabase#open()
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
     * @param id
     * @return
     * @see org.geogit.storage.ObjectDatabase#exists(org.geogit.api.ObjectId)
     */
    @Override
    public boolean exists(ObjectId id) {
        return objects.containsKey(id);
    }

    /**
     * @param objectId
     * @return
     * @see org.geogit.storage.ObjectDatabase#delete(org.geogit.api.ObjectId)
     */
    @Override
    public boolean delete(ObjectId objectId) {
        return objects.remove(objectId) != null;
    }

    /**
     * @param raw
     * @return
     * @see org.geogit.storage.AbstractObjectDatabase#lookUpInternal(byte[])
     */
    @Override
    protected List<ObjectId> lookUpInternal(byte[] raw) {
        throw new UnsupportedOperationException("we override lookup directly");
    }

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
