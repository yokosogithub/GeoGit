/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.geogit.api.RevTree;
import org.geogit.storage.StagingDatabase;
import org.opengis.util.ProgressListener;

public interface StagingArea {

    public StagingDatabase getDatabase();

    /**
     * Marks the object (tree or feature) addressed by {@code path} as an unstaged delete.
     * 
     * @param featurePath
     * @return
     */
    public abstract boolean deleted(final String featurePath);

    /**
     * Inserts the given objects into the index database and marks them as unstaged.
     * 
     * @param objects list of blobs to be batch inserted as unstaged, as [Object writer, bounds,
     *        path]
     * @return list of inserted blob references,or the empty list of the process was cancelled by
     *         the listener
     * @throws Exception
     */
    public void insert(final String treePath, final Iterator<RevFeature> features,
            final ProgressListener progress, final @Nullable Integer size,
            @Nullable final List<NodeRef> target) throws Exception;

    public NodeRef insert(final String parentTreePath, final RevFeature feature);

    /**
     * Stages the object addressed by {@code pathFilter}, or all unstaged objects if
     * {@code pathFilter == null} to be added, if it is/they are marked as an unstaged change. Does
     * nothing otherwise.
     * <p>
     * To stage changes not yet staged, a diff tree walk is performed using the current staged
     * {@link RevTree} as the old object and the current unstaged {@link RevTree} as the new object.
     * Then all the differences are traversed and the staged tree is updated with the changes
     * reported by the diff walk (neat).
     * </p>
     * 
     * @param pathFilter
     * @param progressListener
     */
    public abstract void stage(ProgressListener progress, final @Nullable String pathFilter);

    /**
     * Discards any staged change.
     * 
     * @REVISIT: should this be implemented through ResetOp (GeoGIT.reset()) instead?
     * @TODO: When we implement transaction management will be the time to discard any needed object
     *        inserted to the database too
     */
    public abstract void reset();

}