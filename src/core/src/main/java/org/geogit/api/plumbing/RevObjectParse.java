/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkArgument;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.inject.Inject;

/**
 * Resolves the reference given by a ref spec to the {@link RevObject} it finally points to,
 * dereferencing symbolic refs as necessary.
 */
public class RevObjectParse extends AbstractGeoGitOp<RevObject> {

    private String refSpec;

    private StagingDatabase indexDb;

    private ObjectId objectId;

    @Inject
    public RevObjectParse(StagingDatabase indexDb) {
        this.indexDb = indexDb;
    }

    public RevObjectParse setRefSpec(final String refSpec) {
        this.objectId = null;
        this.refSpec = refSpec;
        return this;
    }

    public RevObjectParse setObjectId(final ObjectId objectId) {
        this.refSpec = null;
        this.objectId = objectId;
        return this;
    }

    /**
     * @return the resolved object id
     * @throws IllegalArgumentException if the provided refspec doesn't resolve to any known object
     */
    @Override
    public RevObject call() throws IllegalArgumentException {
        ObjectId resolvedObjectId;
        if (objectId == null) {
            resolvedObjectId = command(RevParse.class).setRefSpec(refSpec).call();
        } else {
            resolvedObjectId = objectId;
        }

        checkArgument(!resolvedObjectId.isNull(),
                String.format("refspec ('%s') did not resolve to any object", refSpec));

        final TYPE type = command(ResolveObjectType.class).setObjectId(resolvedObjectId).call();
        ObjectSerialisingFactory factory = indexDb.getSerialFactory();
        ObjectReader<? extends RevObject> reader;
        switch (type) {
        case FEATURE:
            throw new UnsupportedOperationException("not yet implemented");
            // break;
        case COMMIT:
            reader = factory.createCommitReader();
            break;
        case TAG:
            throw new UnsupportedOperationException("not yet implemented");
            // break;
        case TREE:
            reader = factory.createRevTreeReader(indexDb);
            break;
        case FEATURETYPE:
            reader = factory.createFeatureTypeReader();
            break;
        default:
            throw new IllegalArgumentException("Unknown object type " + type);
        }

        RevObject revObject = indexDb.get(resolvedObjectId, reader);
        return revObject;
    }
}
