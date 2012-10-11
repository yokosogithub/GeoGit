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

import com.google.common.base.Optional;

/**
 * Resolves the given "ref spec" to a tree id in the repository's object database.
 */
public class ResolveTreeish extends AbstractGeoGitOp<Optional<ObjectId>> {

    private String treeishRefSpec;

    public ResolveTreeish() {
    }

    public ResolveTreeish setTreeish(String treeishRefSpec) {
        checkNotNull(treeishRefSpec);

        this.treeishRefSpec = treeishRefSpec;
        return this;
    }

    @Override
    public Optional<ObjectId> call() {
        checkState(treeishRefSpec != null, "tree-ish ref spec not set");

        Optional<ObjectId> resolved = command(RevParse.class).setRefSpec(treeishRefSpec).call();

        return call(resolved);
    }

    /**
     * @param resolved
     * @return
     */
    private Optional<ObjectId> call(Optional<ObjectId> resolved) {
        if (!resolved.isPresent()) {
            return Optional.absent();
        }

        ObjectId objectId = resolved.get();
        if (objectId.isNull()) {
            return resolved;
        }

        final TYPE objectType = command(ResolveObjectType.class).setObjectId(objectId).call();

        switch (objectType) {
        case TREE:
            // ok
            break;
        case COMMIT: {
            Optional<RevCommit> commit = command(RevObjectParse.class).setObjectId(objectId).call(
                    RevCommit.class);
            if (commit.isPresent()) {
                objectId = commit.get().getTreeId();
            } else {
                objectId = null;
            }
            break;
        }
        case TAG: {
            Optional<RevTag> tag = command(RevObjectParse.class).setObjectId(objectId).call(
                    RevTag.class);
            if (tag.isPresent()) {
                ObjectId commitId = tag.get().getCommitId();
                return call(Optional.of(commitId));
            }
        }
        default:
            throw new IllegalArgumentException(String.format(
                    "Provided ref spec ('%s') doesn't resolve to a tree-ish object: %s",
                    treeishRefSpec, String.valueOf(objectType)));
        }

        return Optional.fromNullable(objectId);
    }
}
