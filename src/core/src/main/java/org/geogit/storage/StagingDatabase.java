/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.NodeRef;

import com.google.common.base.Optional;

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

    public abstract void putUnstaged(NodeRef entry);

    public abstract void stage(NodeRef entry);

    public abstract int countUnstaged(@Nullable String pathFilter);

    public abstract int countStaged(@Nullable String pathFilter);

    public abstract Iterator<NodeRef> getUnstaged(@Nullable String pathFilter);

    public abstract Iterator<NodeRef> getStaged(@Nullable String pathFilter);

    public abstract int removeStaged(String pathFilter);

    public abstract int removeUnStaged(String pathFilter);

    public abstract Optional<NodeRef> findStaged(String path);

    public abstract Optional<NodeRef> findUnstaged(String path);

}