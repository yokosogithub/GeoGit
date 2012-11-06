/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffTreeWalk;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Compares the features in the {@link WorkingTree working tree} and the {@link StagingArea index}
 * or a given root tree-ish.
 */
public class DiffWorkTree extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private StagingArea index;

    private WorkingTree workTree;

    private String pathFilter;

    private ObjectSerialisingFactory serialFactory;

    private String refSpec;

    /**
     * Constructs a new instance of the {@code DiffWorkTree} operation with the given parameters.
     * 
     * @param index the staging area
     * @param workTree the working tree
     * @param serialFactory the serialization factory
     */
    @Inject
    public DiffWorkTree(StagingArea index, WorkingTree workTree,
            ObjectSerialisingFactory serialFactory) {
        this.index = index;
        this.workTree = workTree;
        this.serialFactory = serialFactory;
    }

    /**
     * @param refSpec the name of the root tree object in the to compare the working tree against.
     *        If {@code null} or not specified, defaults to the current state of the index.
     * @return {@code this}
     */
    public DiffWorkTree setOldVersion(@Nullable String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    /**
     * @param path the path filter to use during the diff operation
     * @return {@code this}
     */
    public DiffWorkTree setFilter(@Nullable String path) {
        pathFilter = path;
        return this;
    }

    /**
     * If no {@link #setOldVersion(String) old version} was set, returns the differences between the
     * working tree and the index, otherwise the differences between the working tree and the
     * specified revision.
     * 
     * @return an iterator to a set of differences between the two trees
     * @see DiffEntry
     */
    @Override
    public Iterator<DiffEntry> call() {

        final Optional<String> ref = Optional.fromNullable(refSpec);

        final RevTree oldTree = ref.isPresent() ? getOldTree() : index.getTree();
        final RevTree newTree = workTree.getTree();

        DiffTreeWalk treeWalk = new DiffTreeWalk(index.getDatabase(), oldTree, newTree,
                serialFactory);
        treeWalk.setFilter(pathFilter);
        return treeWalk.get();
    }

    /**
     * @return the tree referenced by the old ref, or the head of the index.
     */
    private RevTree getOldTree() {

        final String oldVersion = Optional.fromNullable(refSpec).or(Ref.STAGE_HEAD);

        ObjectId headTreeId = command(ResolveTreeish.class).setTreeish(oldVersion).call().get();
        final RevTree headTree;
        if (headTreeId.isNull()) {
            headTree = RevTree.NULL;
        } else {
            headTree = command(RevObjectParse.class).setObjectId(headTreeId).call(RevTree.class)
                    .get();
        }

        return headTree;
    }

}
