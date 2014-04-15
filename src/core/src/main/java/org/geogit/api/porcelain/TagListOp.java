/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.RevTag;
import org.geogit.api.plumbing.ForEachRef;
import org.geogit.api.plumbing.RevObjectParse;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Returns a list of all tags
 * 
 */
public class TagListOp extends AbstractGeoGitOp<ImmutableList<RevTag>> {

    @Inject
    public TagListOp() {

    }

    public ImmutableList<RevTag> call() {

        final Predicate<Ref> filter = new Predicate<Ref>() {
            @Override
            public boolean apply(Ref input) {
                return input.getName().startsWith(Ref.TAGS_PREFIX);
            }
        };

        List<Ref> refs = Lists.newArrayList(command(ForEachRef.class).setFilter(filter).call());
        List<RevTag> list = Lists.newArrayList();
        for (Ref ref : refs) {
            Optional<RevTag> tag = command(RevObjectParse.class).setObjectId(ref.getObjectId())
                    .call(RevTag.class);
            list.add(tag.get());
        }
        return ImmutableList.copyOf(list);
    }

}
