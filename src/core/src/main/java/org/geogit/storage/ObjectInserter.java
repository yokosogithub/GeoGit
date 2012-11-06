/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.ObjectId;

/**
 * Encapsulates a transaction.
 * <p>
 * Use the same ObjectInserter for a single transaction
 * </p>
 * 
 * @author groldan
 * 
 */
public class ObjectInserter {

    private ObjectDatabase objectDb;

    // TODO: transaction management
    /**
     * Constructs a new {@code ObjectInserter} with the given {@link ObjectDatabase}.
     * 
     * @param objectDatabase the database to insert to
     */
    public ObjectInserter(ObjectDatabase objectDatabase) {
        objectDb = objectDatabase;
    }

    /**
     * Inserts a provided object into the database.
     * 
     * @param objectID the {@link ObjectId id} of the object to insert
     * @param writer the object writer for the object
     */
    public void insert(final ObjectId objectID, final ObjectWriter<?> writer) {
        objectDb.put(objectID, writer);
    }

}
