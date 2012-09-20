/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.InputStream;

import org.geogit.api.ObjectId;

import com.vividsolutions.jts.geom.GeometryFactory;

public interface ObjectReader<T> {

    /**
     * Hint of type {@link GeometryFactory}
     */
    public static final String JTS_GEOMETRY_FACTORY = "JTS_GEOMETRY_FACTORY";

    /**
     * Hint of type Boolean
     */
    public static final String USE_PROVIDED_FID = "USE_PROVIDED_FID";

    /**
     * @param id
     * @param rawData
     * @return
     * @throws IllegalArgumentException if the provided stream does not represents an object of the
     *         required type
     */
    public T read(ObjectId id, InputStream rawData) throws IllegalArgumentException;

}
