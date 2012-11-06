/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides an interface for writing objects to a given output stream.
 * 
 * @param <T> the object type
 */
public interface ObjectWriter<T> {

    /**
     * Writes the object to the given output stream. Does not close the output stream, as it doesn't
     * belong to this object. The calling code is responsible of the outputstream life cycle.
     * 
     * @param out the stream to write to
     * @throws IOException
     */
    public void write(OutputStream out) throws IOException;

    public T object();
}
