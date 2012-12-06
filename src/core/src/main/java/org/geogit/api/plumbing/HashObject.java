/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hasher;

/**
 * Hashes a RevObject and returns the ObjectId.
 * 
 * @see RevObject
 * @see ObjectId#HASH_FUNCTION
 */
public class HashObject extends AbstractGeoGitOp<ObjectId> {

    private RevObject object;

    /**
     * @param object {@link RevObject} to hash.
     * @return {@code this}
     */
    public HashObject setObject(RevObject object) {
        this.object = object;
        return this;
    }

    /**
     * Hashes a RevObject using a SHA1 hasher.
     * 
     * @return a new ObjectId created from the hash of the RevObject.
     */
    @Override
    public ObjectId call() {
        Preconditions.checkState(object != null, "Object has not been set.");

        final Hasher hasher = ObjectId.HASH_FUNCTION.newHasher();

        switch (object.getType()) {
        case COMMIT:
            HashObjectFunnels.commitFunnel().funnel((RevCommit) object, hasher);
            break;
        case TREE:
            HashObjectFunnels.treeFunnel().funnel((RevTree) object, hasher);
            break;
        case FEATURE:
            HashObjectFunnels.featureFunnel().funnel((RevFeature) object, hasher);
            break;
        case TAG:
            HashObjectFunnels.tagFunnel().funnel((RevTag) object, hasher);
            break;
        case FEATURETYPE:
            HashObjectFunnels.featureTypeFunnel().funnel((RevFeatureType) object, hasher);
            break;
        }

        final byte[] rawKey = hasher.hash().asBytes();
        final ObjectId id = new ObjectId(rawKey);

        return id;
    }

}
