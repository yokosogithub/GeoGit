/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
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