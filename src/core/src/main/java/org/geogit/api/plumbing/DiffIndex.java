/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffTreeWalk;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Compares content and metadata links of blobs between the index and repository
 */
public class DiffIndex extends AbstractGeoGitOp<Iterator<DiffEntry>> implements
        Supplier<Iterator<DiffEntry>> {

    private String refSpec;

    private final List<String> pathFilters = Lists.newLinkedList();

    private boolean reportTrees;

    /**
     * Constructs a new {@code DiffIndex}.
     */
    @Inject
    public DiffIndex() {
    }

    /**
     * @param pathFilter the path filter to use during the diff operation
     * @return {@code this}
     */
    public DiffIndex setFilter(@Nullable List<String> pathFilter) {
        this.pathFilters.clear();
        if (pathFilter != null) {
            this.pathFilters.addAll(pathFilter);
        }
        return this;
    }

    public DiffIndex addFilter(@Nullable String pathFilter) {
        if (pathFilter != null) {
            this.pathFilters.add(pathFilter);
        }
        return this;
    }

    /**
     * @param refSpec the name of the root tree object in the repository's object database to
     *        compare the index against. If {@code null} or not specified, defaults to the tree
     *        object of the current HEAD commit.
     * @return {@code this}
     */
    public DiffIndex setOldVersion(@Nullable String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    /**
     * Finds differences between the tree pointed to by the given ref and the index.
     * 
     * @return an iterator to a set of differences between the two trees
     * @see DiffEntry
     */
    @Override
    public Iterator<DiffEntry> call() {
        final String oldVersion = Optional.fromNullable(refSpec).or(Ref.HEAD);
        final Optional<ObjectId> rootTreeId;
        rootTreeId = command(ResolveTreeish.class).setTreeish(oldVersion).call();
        Preconditions.checkArgument(rootTreeId.isPresent(), "refSpec did not resolve to a tree");

        final RevTree rootTree;
        if (rootTreeId.get().isNull()) {
            rootTree = RevTree.EMPTY;
        } else {
            rootTree = command(RevObjectParse.class).setObjectId(rootTreeId.get())
                    .call(RevTree.class).get();
        }

        final RevTree newTree = getIndex().getTree();

        DiffTreeWalk treeWalk = new DiffTreeWalk(getIndex().getDatabase(), rootTree, newTree);
        treeWalk.setFilter(this.pathFilters);
        treeWalk.setReportTrees(reportTrees);
        return treeWalk.get();
    }

    /**
     * @param reportTrees
     * @return
     */
    public DiffIndex setReportTrees(boolean reportTrees) {
        this.reportTrees = reportTrees;
        return this;
    }

    /**
     * Implements {@link Supplier#get()} by deferring to {@link #call()}
     * 
     * @see #call()
     */
    @Override
    public Iterator<DiffEntry> get() {
        return call();
    }
}
