/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffTreeIterator;
import org.geogit.api.plumbing.diff.DiffTreeView;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * Compares the features in the {@link WorkingTree working tree} and the {@link StagingArea index}
 * or a given root tree-ish.
 */
public class DiffWorkTree extends AbstractGeoGitOp<Iterator<DiffEntry>> {

    private StagingDatabase indexDb;

    private String pathFilter;

    private ObjectSerialisingFactory serialFactory;

    private String refSpec;

    @Inject
    public DiffWorkTree(StagingDatabase index, ObjectSerialisingFactory serialFactory) {
        this.indexDb = index;
        this.serialFactory = serialFactory;
    }

    /**
     * @param oldRefSpec
     */
    public DiffWorkTree setOldVersion(@Nullable String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    public DiffWorkTree setFilter(@Nullable String path) {
        pathFilter = path;
        return this;
    }

    /**
     * If no {@link #setOldVersion(String) old version} was set, returns the differences between the
     * working tree and the index, otherwise the differences between the working tree and the
     * specified revision.
     */
    @Override
    public Iterator<DiffEntry> call() {

        Iterator<NodeRef> unstaged = indexDb.getUnstaged(pathFilter);

        RevTree stagedTree = getOldTree();
        return new DiffTreeIterator(indexDb, stagedTree, unstaged, serialFactory);
    }

    /**
     * @return
     */
    private RevTree getOldTree() {

        final String oldVersion = Optional.fromNullable(refSpec).or(Ref.HEAD);

        ObjectId headTreeId = command(ResolveTreeish.class).setTreeish(oldVersion).call().get();
        final RevTree headTree;
        if (headTreeId.isNull()) {
            headTree = RevTree.NULL;
        } else {
            headTree = command(RevObjectParse.class).setObjectId(headTreeId).call(RevTree.class).get();
        }

        Iterator<NodeRef> staged = indexDb.getStaged(pathFilter);
        Map<String, NodeRef> changes = toMap(staged);
        String rootPath = "";
        return new DiffTreeView(indexDb, rootPath, headTree, changes);
    }

    private Map<String, NodeRef> toMap(Iterator<NodeRef> changes) {
        Function<NodeRef, String> keyFunction = new Function<NodeRef, String>() {
            @Override
            public String apply(NodeRef input) {
                return input.getPath();
            }
        };
        ImmutableMap<String, NodeRef> changesMap = Maps.uniqueIndex(changes, keyFunction);
        return changesMap;
    }

}
