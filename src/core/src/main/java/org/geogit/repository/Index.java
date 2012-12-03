/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.plumbing.DiffCount;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.FindOrCreateSubtree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.WriteBack;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;

/**
 * The Index keeps track of the changes that have been staged, but not yet committed to the
 * repository.
 * <p>
 * The Index uses an {@link StagingDatabase object database} as storage for the staged changes. This
 * allows for really large operations not to eat up too much heap, and also works better and allows
 * for easier implementation of operations that need to manipulate the index.
 * <p>
 * The Index database is a composite of its own ObjectDatabase and the repository's. Object look ups
 * against the index first search on the index db, and if not found defer to the repository object
 * db.
 * <p>
 * Internally, finding out what changes are unstaged is a matter of comparing (through a diff tree
 * walk) the working tree and the staged changes tree. And finding out what changes are staged to be
 * committed is performed through a diff tree walk comparing the staged changes tree and the
 * repository's head tree.
 * 
 */
public class Index implements StagingArea {

    @Inject
    private Repository repository;

    @Inject
    private StagingDatabase indexDatabase;

    @Inject
    private ObjectSerialisingFactory serialFactory;

    /**
     * @return the staging database.
     */
    @Override
    public StagingDatabase getDatabase() {
        return indexDatabase;
    }

    /**
     * Updates the STAGE_HEAD ref to the specified tree.
     * 
     * @param newTree the tree to set as the new STAGE_HEAD
     */
    @Override
    public void updateStageHead(ObjectId newTree) {
        repository.command(UpdateRef.class).setName(Ref.STAGE_HEAD).setNewValue(newTree).call();
    }

    /**
     * @return the tree represented by STAGE_HEAD. If there is no tree set at STAGE_HEAD, it will
     *         return the HEAD tree (no unstaged changes).
     */
    @Override
    public RevTree getTree() {
        Optional<ObjectId> stageTreeId = repository.command(ResolveTreeish.class)
                .setTreeish(Ref.STAGE_HEAD).call();
        final RevTree stageTree;
        if (!stageTreeId.isPresent() || stageTreeId.get().isNull()) {
            // Work tree was not resolved, update it to the head.
            RevTree headTree = repository.getOrCreateHeadTree();
            updateStageHead(headTree.getId());
            stageTree = headTree;

        } else {
            stageTree = repository.command(RevObjectParse.class).setObjectId(stageTreeId.get())
                    .call(RevTree.class).or(RevTree.EMPTY);
        }
        return stageTree;
    }

    /**
     * @return a supplier for the index.
     */
    private Supplier<RevTreeBuilder> getTreeSupplier() {
        Supplier<RevTreeBuilder> supplier = new Supplier<RevTreeBuilder>() {
            @Override
            public RevTreeBuilder get() {
                return getTree().builder(getDatabase());
            }
        };
        return Suppliers.memoize(supplier);
    }

    /**
     * @param path the path of the {@link Node} to find
     * @return the {@code Node} for the feature at the specified path if it exists in the index,
     *         otherwise {@link Optional#absent()}
     */
    @Override
    public Optional<Node> findStaged(final String path) {
        Optional<NodeRef> entry = repository.command(FindTreeChild.class).setIndex(true)
                .setParent(getTree()).setChildPath(path).call();
        if (entry.isPresent()) {
            return Optional.of(entry.get().getNode());
        } else {
            return Optional.absent();
        }
    }

    /**
     * Stages the changes indicated by the {@link DiffEntry} iterator.
     * 
     * @param progress the progress listener for the process
     * @param unstaged an iterator for the unstaged changes
     * @param numChanges number of unstaged changes
     */
    @Override
    public void stage(final ProgressListener progress, final Iterator<DiffEntry> unstaged,
            final long numChanges) {
        int i = 0;
        progress.started();
        // System.err.println("staging with path: " + path2 + ". Matches: " + numChanges);
        Map<String, List<DiffEntry>> changeMap = new HashMap<String, List<DiffEntry>>();
        while (unstaged.hasNext()) {
            DiffEntry diff = unstaged.next();
            NodeRef oldObject = diff.getOldObject();
            NodeRef newObject = diff.getNewObject();
            String path;
            if (newObject == null) {
                // Delete
                path = oldObject.getParentPath();
            } else {
                path = newObject.getParentPath();
            }
            List<DiffEntry> changeList = changeMap.get(path);
            if (changeList == null) {
                changeList = new LinkedList<DiffEntry>();
            }

            changeList.add(diff);
            changeMap.put(path, changeList);

        }

        Iterator<Map.Entry<String, List<DiffEntry>>> changes = changeMap.entrySet().iterator();
        while (changes.hasNext()) {
            Map.Entry<String, List<DiffEntry>> pairs = changes.next();

            RevTreeBuilder parentTree = repository.command(FindOrCreateSubtree.class)
                    .setParent(Suppliers.ofInstance(Optional.of(getTree()))).setIndex(true)
                    .setChildPath(pairs.getKey()).call().builder(getDatabase());

            for (DiffEntry diff : pairs.getValue()) {
                i++;
                progress.progress((float) (i * 100) / numChanges);

                NodeRef oldObject = diff.getOldObject();
                NodeRef newObject = diff.getNewObject();
                if (newObject == null) {
                    // Delete
                    parentTree.remove(oldObject.name());
                } else if (oldObject == null) {
                    // Add
                    parentTree.put(newObject.getNode());
                } else {
                    // Modify
                    parentTree.put(newObject.getNode());
                }
            }

            ObjectId newTree = repository.command(WriteBack.class).setAncestor(getTreeSupplier())
                    .setChildPath(pairs.getKey()).setToIndex(true).setTree(parentTree.build())
                    .call();

            updateStageHead(newTree);
        }
        progress.complete();
    }

    /**
     * @param pathFilter if specified, only changes that match the filter will be returned
     * @return an iterator for all of the differences between STAGE_HEAD and HEAD based on the path
     *         filter.
     */
    @Override
    public Iterator<DiffEntry> getStaged(final @Nullable String pathFilter) {
        Iterator<DiffEntry> unstaged = repository.command(DiffIndex.class).setFilter(pathFilter)
                .call();
        return unstaged;
    }

    /**
     * @param pathFilter if specified, only changes that match the filter will be returned
     * @return the number differences between STAGE_HEAD and HEAD based on the path filter.
     */
    @Override
    public long countStaged(final @Nullable String pathFilter) {
        Long count = repository.command(DiffCount.class).setOldVersion(Ref.HEAD)
                .setNewVersion(Ref.STAGE_HEAD).setFilter(pathFilter).call();
        return count.longValue();
    }

    /**
     * Discards any staged change.
     */
    // @REVISIT: should this be implemented through ResetOp (GeoGIT.reset()) instead?
    // @TODO: When we implement transaction management will be the time to discard any needed object
    // inserted to the database too
    @Override
    public void reset() {
        // Reset STAGE_HEAD to the HEAD tree
        RevTree headTree = repository.getOrCreateHeadTree();
        updateStageHead(headTree.getId());
    }
}
