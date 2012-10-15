/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffTreeIterator;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Compares content and metadata links of blobs between the index and repository
 */
public class DiffIndex extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private ObjectDatabase repoDb;

    private StagingDatabase indexDb;

    private ObjectSerialisingFactory serialFactory;

    private String refSpec;

    private String pathFilter;

    @Inject
    public DiffIndex(ObjectDatabase repoDb, StagingDatabase index,
            ObjectSerialisingFactory serialFactory) {
        this.repoDb = repoDb;
        this.indexDb = index;
        this.serialFactory = serialFactory;
    }

    /**
     * @param pathFilter
     */
    public DiffIndex setFilter(@Nullable String pathFilter) {
        this.pathFilter = pathFilter;
        return this;
    }

    /**
     * @param the name of the root tree object in the repository's object database to compare the
     *        index against. If {@code null} or not specified, defaults to the tree object of the
     *        current HEAD commit.
     */
    public DiffIndex setOldVersion(@Nullable String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    @Override
    public Iterator<DiffEntry> call() {
        final String oldVersion = Optional.fromNullable(refSpec).or(Ref.HEAD);
        final ObjectId rootTreeId = command(ResolveTreeish.class).setTreeish(oldVersion).call();

        final RevTree rootTree;
        if (rootTreeId.isNull()) {
            rootTree = RevTree.NULL;
        } else {
            rootTree = (RevTree) command(RevObjectParse.class).setObjectId(rootTreeId).call();
        }

        Iterator<NodeRef> staged = indexDb.getStaged(pathFilter);
        return new DiffTreeIterator(repoDb, rootTree, staged, serialFactory);
    }
}
