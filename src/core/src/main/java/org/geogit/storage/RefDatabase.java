/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.storage;

import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;

/**
 * @author groldan
 * 
 */
public interface RefDatabase {

    public abstract void create();

    public abstract void close();

    public abstract Ref getRef(String name);

    public abstract List<Ref> getRefs(String prefix);

    public abstract List<Ref> getRefsPontingTo(ObjectId oid);

    /**
     * @param ref
     * @return {@code true} if the ref was inserted, {@code false} if it already existed and pointed
     *         to the same object
     */
    public abstract boolean put(Ref ref);

}