/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Collections;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.ForEachRef;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Creates a new head ref (branch) pointing to the specified tree-ish or the current HEAD if no
 * tree-ish was specified.
 * <p>
 * 
 * @TODO: support branch descriptions
 * @TODO: support setting up the branch to track a remote branch
 */
public class BranchListOp extends AbstractGeoGitOp<ImmutableList<Ref>> {

    private boolean remotes;

    private boolean locals;

    @Inject
    public BranchListOp() {
        locals = true;
        remotes = false;
    }

    public BranchListOp setRemotes(boolean remotes) {
        this.remotes = remotes;
        return this;
    }

    public BranchListOp setLocal(boolean locals) {
        this.locals = locals;
        return this;
    }

    public ImmutableList<Ref> call() {

        final Predicate<Ref> filter = new Predicate<Ref>() {
            @Override
            public boolean apply(Ref input) {
                if (locals && input.getName().startsWith(Ref.HEADS_PREFIX)) {
                    return true;
                }
                if (remotes && input.getName().startsWith(Ref.REMOTES_PREFIX)) {
                    return true;
                }
                return false;
            }
        };

        List<Ref> refs = Lists.newArrayList(command(ForEachRef.class).setFilter(filter).call());
        Collections.sort(refs);
        return ImmutableList.copyOf(refs);
    }

}
