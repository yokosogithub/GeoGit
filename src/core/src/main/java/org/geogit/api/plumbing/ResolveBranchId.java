/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

/**
 * Given an id, returns the ref that points to that id, if it exists
 * 
 */
public class ResolveBranchId extends AbstractGeoGitOp<Optional<Ref>> {

    private ObjectId id;

    public ResolveBranchId setObjectId(ObjectId id) {
        this.id = id;
        return this;
    }

    @Override
    public Optional<Ref> call() {
        Preconditions.checkState(id != null, "id has not been set.");
        Predicate<Ref> filter = new Predicate<Ref>() {
            @Override
            public boolean apply(@Nullable Ref ref) {
                return ref.getObjectId().equals(id);
            }
        };
        ImmutableSet<Ref> refs = command(ForEachRef.class).setFilter(filter).call();
        if (refs.isEmpty()) {
            return Optional.absent();
        } else {
            return Optional.of(refs.iterator().next());
        }
    }
}