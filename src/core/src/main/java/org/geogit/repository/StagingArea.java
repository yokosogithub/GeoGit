/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.NodeRef;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.storage.StagingDatabase;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;

/**
 * Serves as an interface for the index of the GeoGit repository.
 * 
 * @see StagingDatabase
 */
public interface StagingArea {

    /**
     * @return the staging database.
     */
    public StagingDatabase getDatabase();

    /**
     * @return the tree represented by STAGE_HEAD. If there is no tree set at STAGE_HEAD, it will
     *         return the HEAD tree (no staged changes).
     */
    public RevTree getTree();

    /**
     * @param path
     * @return the NodeRef for the feature at the specified path if it exists in the index,
     *         otherwise Optional.absent()
     */
    public abstract Optional<NodeRef> findStaged(final String path);

    /**
     * Stages the changes indicated by the {@link DiffEntry} iterator.
     * 
     * @param progress
     * @param unstaged
     * @param numChanges
     */
    public abstract void stage(final ProgressListener progress, final Iterator<DiffEntry> unstaged,
            final long numChanges);

    /**
     * @param pathFilter
     * @return an iterator for all of the differences between STAGE_HEAD and HEAD based on the path
     *         filter.
     */
    public abstract Iterator<DiffEntry> getStaged(final @Nullable String pathFilter);

    /**
     * @param pathFilter
     * @return the number differences between STAGE_HEAD and HEAD based on the path filter.
     */
    public abstract long countStaged(final @Nullable String pathFilter);

    /**
     * Discards any staged change.
     */
    // REVISIT: should this be implemented through ResetOp (GeoGIT.reset()) instead?
    // TODO: When we implement transaction management will be the time to discard any needed object
    // inserted to the database too
    public abstract void reset();

}