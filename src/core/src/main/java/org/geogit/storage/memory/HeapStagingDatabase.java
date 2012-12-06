/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.ning.compress.lzf.LZFInputStream;

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
            final ObjectSerialisingFactory serialFactory) {
        super(serialFactory);
        this.repositoryDb = repositoryDb;
    }

    // /////////////////////////////////////////
    /**
     * 
     * @see org.geogit.storage.StagingDatabase#open()
     */
    @Override
    public void open() {
        super.open();
    }

    /**
     * @see org.geogit.storage.StagingDatabase#close()
     */
    @Override
    public void close() {
        super.close();
    }

    @Override
    public boolean exists(ObjectId id) {
        boolean exists = super.exists(id);
        if (!exists) {
            exists = repositoryDb.exists(id);
        }
        return exists;
    }

    /**
     * Gets the raw input stream of the object with the given {@link ObjectId id}.
     * 
     * @param id the id of the object to get
     * @return the input stream of the object
     */
    @Override
    public final InputStream getRaw(final ObjectId id) throws IllegalArgumentException {
        InputStream in = getRawInternal(id);
        if (in != null) {
            try {
                return new LZFInputStream(in);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return repositoryDb.getRaw(id);
    }

    @Override
    protected InputStream getRawInternal(ObjectId id) throws IllegalArgumentException {
        if (super.exists(id)) {
            return super.getRawInternal(id);
        }
        return null;
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
        if (super.exists(id)) {
            return super.get(id, type);
        }
        return repositoryDb.get(id, type);
    }

    @Override
    public RevObject get(ObjectId id) {
        if (super.exists(id)) {
            return super.get(id);
        }
        return repositoryDb.get(id);
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

}
