/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.inject.Inject;

/**
 *
 */
public class ResolveObjectType extends AbstractGeoGitOp<RevObject.TYPE> {

    private StagingDatabase indexDb;

    private ObjectId oid;

    @Inject
    public ResolveObjectType(StagingDatabase indexDb) {
        this.indexDb = indexDb;
    }

    public ResolveObjectType setObjectId(ObjectId oid) {
        this.oid = oid;
        return this;
    }

    /**
     * @throws IllegalArgumentException if the object doesn't exist
     */
    @Override
    public TYPE call() throws IllegalArgumentException {
        ObjectSerialisingFactory serialFactory = indexDb.getSerialFactory();
        TYPE type = indexDb.get(oid, serialFactory.createObjectTypeReader());
        return type;
    }
}
