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

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
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
 * The Index keeps track of the changes not yet committed to the repository.
 * <p>
 * The Index uses an {@link StagingDatabase object database} as storage for the staged and unstaged
 * changes. This allows for really large operations not to eat up too much heap, and also works
 * better and allows for easier implementation of operations that need to manipulate the index.
 * <p>
 * The Index database is a composite of its own ObjectDatabase and the repository's. Object look ups
 * against the index first search on the index db, and if not found defer to the repository object
 * db.
 * <p>
 * The index holds references to two trees of its own, one for the staged changes and one for the
 * unstaged ones. Modifications to the working tree shall update the Index unstaged changes tree
 * through the {@link #inserted(Iterator) inserted} and {@link #deleted(String...) deleted} methods
 * (an object update is just another insert as far as GeoGit is concerned).
 * <p>
 * Marking unstaged changes to be committed is made through the {@link #stage(String...)} method.
 * <p>
 * Internally, finding out what changes are unstaged is a matter of comparing (through a diff tree
 * walk) the unstaged changes tree and the staged changes tree. And finding out what changes are
 * staged to be committed is performed through a diff tree walk comparing the staged changes tree
 * and the repository's head tree (or any other repository tree reference given to
 * {@link #writeTree(NodeRef)}).
 * <p>
 * When staged changes are to be committed to the repository, the {@link #writeTree(NodeRef)} method
 * shall be called with a reference to the repository root tree that the staged changes tree is to
 * be compared against (usually the HEAD tree ref).
 * 
 * @author Gabriel Roldan
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
    private void updateStageHead(ObjectId newTree) {
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
                    .call(RevTree.class).or(RevTree.NULL);
        }
        return stageTree;
    }

    /**
     * @return a supplier for the index.
     */
    private Supplier<MutableTree> getTreeSupplier() {
        Supplier<MutableTree> supplier = new Supplier<MutableTree>() {
            @Override
            public MutableTree get() {
                return getTree().mutable();
            }
        };
        return Suppliers.memoize(supplier);
    }

    /**
     * @param path the path of the {@link NodeRef} to find
     * @return the {@code NodeRef} for the feature at the specified path if it exists in the index,
     *         otherwise {@code Optional.absent()}
     */
    @Override
    public Optional<NodeRef> findStaged(final String path) {
        Optional<NodeRef> entry = repository.command(FindTreeChild.class).setIndex(true)
                .setParent(getTree()).setChildPath(path).call();
        return entry;
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
            final int numChanges) {
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
                path = NodeRef.parentPath(oldObject.getPath());
            } else {
                path = NodeRef.parentPath(newObject.getPath());
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

            MutableTree parentTree = repository.command(FindOrCreateSubtree.class)
                    .setParent(Suppliers.ofInstance(Optional.of(getTree()))).setIndex(true)
                    .setChildPath(pairs.getKey()).call().mutable();

            for (DiffEntry diff : pairs.getValue()) {
                i++;
                progress.progress((float) (i * 100) / numChanges);

                NodeRef oldObject = diff.getOldObject();
                NodeRef newObject = diff.getNewObject();
                if (newObject == null) {
                    // Delete
                    parentTree.remove(oldObject.getPath());
                } else if (oldObject == null) {
                    // Add
                    parentTree.put(newObject);
                } else {
                    // Modify
                    parentTree.put(newObject);
                }
            }

            ObjectId newTree = repository.command(WriteBack.class).setAncestor(getTreeSupplier())
                    .setChildPath(pairs.getKey()).setIndex(true).setTree(parentTree).call();

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
    public int countStaged(final @Nullable String pathFilter) {
        Iterator<DiffEntry> unstaged = getStaged(pathFilter);
        int count = 0;
        while (unstaged.hasNext()) {
            count++;
            unstaged.next();
        }
        return count;
    }

    /**
     * Discards any staged change.
     * 
     * @REVISIT: should this be implemented through ResetOp (GeoGIT.reset()) instead?
     * @TODO: When we implement transaction management will be the time to discard any needed object
     *        inserted to the database too
     */
    @Override
    public void reset() {
        // Reset STAGE_HEAD to the HEAD tree
        RevTree headTree = repository.getOrCreateHeadTree();
        updateStageHead(headTree.getId());
    }
}
