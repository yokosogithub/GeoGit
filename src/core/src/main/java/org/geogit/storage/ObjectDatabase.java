/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.InputStream;
import java.util.List;

import org.geogit.api.ObjectId;

/**
 * Provides an interface for implementations of GeoGit object databases.
 */
public interface ObjectDatabase {

    /**
     * Initializes/opens the databse. It's safe to call this method multiple times, and only the
     * first call shall take effect.
     */
    public void open();

    /**
     * @return true if the database is open, false otherwise
     */
    public boolean isOpen();

    /**
     * Closes the database.
     */
    public void close();

    /**
     * Determines if the given {@link ObjectId} exists in the object database.
     * 
     * @param id the id to search for
     * @return true if the object exists, false otherwise
     */
    public boolean exists(final ObjectId id);

    /**
     * Gets the raw input stream of the object with the given {@link ObjectId id}.
     * 
     * @param id the id of the object to get
     * @return the input stream of the object
     */
    public InputStream getRaw(final ObjectId id);

    /**
     * Searches the database for {@link ObjectId}s that match the given partial id.
     * 
     * @param partialId the partial id to search for
     * @return a list of matching results
     */
    public List<ObjectId> lookUp(final String partialId);

    /**
     * Reads an object with the given {@link ObjectId id} out of the database.
     * 
     * @param id the id of the object to read
     * @param reader the reader of the object
     * @return the object, as read in from the {@link ObjectReader}
     */
    public <T> T get(final ObjectId id, final ObjectReader<T> reader);

    /**
     * 
     */
    // public <T> ObjectId put(final ObjectWriter<T> writer);

    /**
     * Adds an object to the database with the given {@link ObjectId id}. If an object with the same
     * id already exists, it will not be inserted.
     * 
     * @param id the id of the object to insert
     * @param writer the writer for the object
     * @return true if the object was inserted, false otherwise
     */
    public boolean put(final ObjectId id, final ObjectWriter<?> writer);

    /**
     * @return a newly constructed {@link ObjectInserter} for this database
     */
    public ObjectInserter newObjectInserter();

    /**
     * Deletes the object with the provided {@link ObjectId id} from the database.
     * 
     * @param objectId the id of the object to delete
     * @return true if the object was deleted, false if it was not found
     */
    public boolean delete(ObjectId objectId);
}