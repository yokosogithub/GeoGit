/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.geogit.api.ObjectId;

public interface ObjectDatabase {

    /**
     * Initializes/opens the databse. It's safe to call this method multiple times, and only the
     * first call shall take effect.
     */
    public void open();

    public boolean isOpen();

    public void close();

    public boolean exists(final ObjectId id);

    /**
     * @param id
     * @return
     * @throws IOException
     * @throws IllegalArgumentException if an object with such id does not exist
     */
    public InputStream getRaw(final ObjectId id);

    public List<ObjectId> lookUp(final String partialId);

    /**
     * @param <T>
     * @param id
     * @param reader
     * @return
     * @throws IOException
     * @throws IllegalArgumentException if an object with such id does not exist
     */
    public <T> T get(final ObjectId id, final ObjectReader<T> reader);

    /**
     * 
     */
    public <T> ObjectId put(final ObjectWriter<T> writer);

    /**
     * @param id
     * @param writer
     * @return {@code true} if the object was inserted and it didn't exist previously, {@code false}
     *         if the object was inserted and it replaced an already existing object with the same
     *         key.
     * @throws Exception
     */
    public boolean put(final ObjectId id, final ObjectWriter<?> writer);

    public ObjectInserter newObjectInserter();

    public boolean delete(ObjectId objectId);

    public ObjectSerialisingFactory getSerialFactory();

}