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
import org.geogit.storage.StagingDatabase;

import com.google.inject.Inject;

/**
 * Compares content and metadata links of blobs between the index and repository
 */
public class DiffIndex extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private ObjectId rootTreeId;

    private String pathFilter;

    private ObjectDatabase repoDb;

    private StagingDatabase indexDb;

    @Inject
    public DiffIndex(ObjectDatabase repoDb, StagingDatabase index) {
        this.repoDb = repoDb;
        this.indexDb = index;
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
     * @see RefParse
     */
    public void setRootTree(@Nullable ObjectId rootTreeId) {
        this.rootTreeId = rootTreeId;
    }

    @Override
    public Iterator<DiffEntry> call() throws Exception {
        final ObjectId treeId;
        if (rootTreeId == null) {
            treeId = command(ResolveTreeish.class).setTreeish(Ref.HEAD).call();
        } else {
            treeId = rootTreeId;
        }

        RevTree rootTree;
        if (treeId.isNull()) {
            rootTree = RevTree.NULL;
        } else {
            rootTree = (RevTree) command(RevObjectParse.class).setObjectId(treeId).call();
        }

        Iterator<NodeRef> staged = indexDb.getStaged(pathFilter);
        return new DiffTreeIterator(repoDb, rootTree, staged);
    }
}
