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

import java.util.Iterator;
import java.util.List;

import org.geogit.api.DiffEntry;

/**
 * @author groldan
 * 
 */
public interface StagingDatabase extends ObjectDatabase {

    public abstract void reset();

    /**
     * Clears the unstaged tree
     */
    public abstract void clearUnstaged();

    /**
     * Clears the staged tree
     */
    public abstract void clearStaged();

    public abstract void putUnstaged(DiffEntry diffEntry);

    public abstract void stage(DiffEntry diffEntry);

    public abstract int countUnstaged(List<String> pathFilter);

    public abstract int countStaged(List<String> pathFilter);

    public abstract Iterator<DiffEntry> getUnstaged(List<String> pathFilter);

    public abstract Iterator<DiffEntry> getStaged(List<String> pathFilter);

    public abstract int removeStaged(List<String> pathFilter);

    public abstract int removeUnStaged(List<String> pathFilter);

    public abstract DiffEntry findStaged(String... path);

    public abstract DiffEntry findStaged(List<String> path);

    public abstract DiffEntry findUnstaged(String... path);

    public abstract DiffEntry findUnstaged(List<String> path);

    public abstract ObjectDatabase getObjectDatabase();

}