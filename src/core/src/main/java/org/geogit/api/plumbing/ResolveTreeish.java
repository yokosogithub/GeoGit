/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTag;

/**
 * Resolves the given "ref spec" to a tree id in the repository's object database
 */
public class ResolveTreeish extends AbstractGeoGitOp<ObjectId> {

    private String treeishRefSpec;

    public ResolveTreeish() {

    }

    public ResolveTreeish setTreeish(String treeishRefSpec) {
        checkNotNull(treeishRefSpec);

        this.treeishRefSpec = treeishRefSpec;
        return this;
    }

    @Override
    public ObjectId call() {
        checkState(treeishRefSpec != null, "tree-ish ref spec not set");

        ObjectId objectId = command(RevParse.class).setRefSpec(treeishRefSpec).call();

        if (!objectId.isNull()) {
            TYPE objectType = command(ResolveObjectType.class).setObjectId(objectId).call();
            switch (objectType) {
            case TREE:
                // ok
                break;
            case COMMIT: {
                RevCommit commit = (RevCommit) command(RevObjectParse.class).setObjectId(objectId)
                        .call();
                objectId = commit.getTreeId();
                break;
            }
            case TAG: {
                RevTag tag = (RevTag) command(RevObjectParse.class).setObjectId(objectId).call();
                ObjectId commitId = tag.getCommitId();
                RevCommit commit = (RevCommit) command(RevObjectParse.class).setObjectId(commitId)
                        .call();
                objectId = commit.getTreeId();
            }
            default:
                throw new IllegalArgumentException(String.format(
                        "Provided ref spec ('%s') doesn't resolve to a tree-ish object: %s",
                        treeishRefSpec, String.valueOf(objectType)));
            }
        }

        return objectId;
    }
}
