/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.RevSHA1Tree;
import org.geogit.storage.StagingDatabase;

import com.google.inject.Inject;

/**
 * Creates a new {@link MutableTree} backed by the specified object database (the repository's by
 * default, or the staging area object database if so indicated)
 */
public class CreateTree extends AbstractGeoGitOp<MutableTree> {

    private boolean index;

    private ObjectDatabase odb;

    private StagingDatabase indexDb;

    @Inject
    public CreateTree(ObjectDatabase odb, StagingDatabase indexDb) {
        this.odb = odb;
        this.indexDb = indexDb;
    }

    /**
     * @param toIndexDb it {@code true}, the returned tree is backed by the {@link StagingDatabase},
     *        otherwise by the repository's {@link ObjectDatabase}
     */
    public CreateTree setIndex(boolean toIndexDb) {
        index = toIndexDb;
        return this;
    }

    @Override
    public MutableTree call() {
        ObjectDatabase storage = index ? indexDb : odb;

        return new RevSHA1Tree(ObjectId.NULL, storage, 0).mutable();
    }

}
