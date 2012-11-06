/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.MutableTree;
import org.geogit.api.ObjectId;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
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

    private ObjectSerialisingFactory serialFactory;

    /**
     * Constructs a new {@code CreateTree} operation with the specified parameters.
     * 
     * @param odb the repository object database
     * @param indexDb the staging database
     * @param serialFactory the serialization factory
     */
    @Inject
    public CreateTree(ObjectDatabase odb, StagingDatabase indexDb,
            ObjectSerialisingFactory serialFactory) {
        this.odb = odb;
        this.indexDb = indexDb;
        this.serialFactory = serialFactory;
    }

    /**
     * @param toIndexDb if {@code true}, the returned tree is backed by the {@link StagingDatabase},
     *        otherwise by the repository's {@link ObjectDatabase}
     * @return {@code this}
     */
    public CreateTree setIndex(boolean toIndexDb) {
        index = toIndexDb;
        return this;
    }

    /**
     * Executes the create tree operation and returns a new mutable tree.
     * 
     * @return the {@link MutableTree} that was created by the operation
     */
    @Override
    public MutableTree call() {
        ObjectDatabase storage = index ? indexDb : odb;

        return new RevSHA1Tree(ObjectId.NULL, storage, 0, serialFactory).mutable();
    }

}
