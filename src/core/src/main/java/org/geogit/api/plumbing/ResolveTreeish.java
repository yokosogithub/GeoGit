/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
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

    private ObjectId treeish;

    /**
     * @param treeishRefSpec a ref spec that ultimately resolves to a tree id
     * @return {@code this}
     */
    public ResolveTreeish setTreeish(String treeishRefSpec) {
        checkNotNull(treeishRefSpec);
        this.treeishRefSpec = treeishRefSpec;
        this.treeish = null;
        return this;
    }

    /**
     * @param treeish an object id that ultimately resolves to a tree id (i.e. a commit id, a tree
     *        id)
     * @return {@code this}
     */
    public ResolveTreeish setTreeish(ObjectId treeish) {
        checkNotNull(treeish);
        this.treeish = treeish;
        this.treeishRefSpec = null;
        return this;
    }

    /**
     * Executes the command.
     * 
     * @return an {@link Optional} of the {@link ObjectId} that was resolved, or
     *         {@link Optional#absent()} if it did not resolve.
     */
    @Override
    public Optional<ObjectId> call() {
        checkState(treeishRefSpec != null || treeish != null, "tree-ish ref spec not set");

        Optional<ObjectId> resolved;
        if (treeishRefSpec != null) {
            resolved = command(RevParse.class).setRefSpec(treeishRefSpec).call();
        } else {
            resolved = Optional.of(treeish);
        }

        return call(resolved);
    }

    /**
     * @param resolved an {@link Optional} with an ObjectId to resolve
     * @return an {@link Optional} of the {@link ObjectId} that was resolved, or
     *         {@link Optional#absent()} if it did not resolve.
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
