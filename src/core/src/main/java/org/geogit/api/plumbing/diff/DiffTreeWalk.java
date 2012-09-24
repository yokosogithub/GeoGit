/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.RevTree;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

/**
 * Composes an {@link Iterator} of {@link DiffEntry} out of two {@link RevTree}ss
 */
public class DiffTreeWalk {

    @Nonnull
    private final RevTree fromRootTree;

    @Nonnull
    private final RevTree toRootTree;

    @Nonnull
    private ObjectDatabase objectDb;

    @Nullable
    private String pathFilter;

    public DiffTreeWalk(final ObjectDatabase db, final RevTree fromRootTree,
            final RevTree toRootTree) {
        Preconditions.checkNotNull(db);
        Preconditions.checkNotNull(fromRootTree);
        Preconditions.checkNotNull(toRootTree);
        this.objectDb = db;
        this.fromRootTree = fromRootTree;
        this.toRootTree = toRootTree;
        this.pathFilter = "";// root
    }

    public void setFilter(final String path) {
        if (path == null || path.isEmpty()) {
            this.pathFilter = "";// root
        } else {
            this.pathFilter = path;
        }
    }

    public Iterator<DiffEntry> get() throws IOException {

        RevTree oldTree = this.fromRootTree;
        RevTree newTree = this.toRootTree;

        boolean pathFiltering = !pathFilter.isEmpty();
        if (pathFiltering) {
            Optional<NodeRef> oldObjectRef = getFilteredObjectRef(fromRootTree);
            Optional<NodeRef> newObjectRef = getFilteredObjectRef(toRootTree);

            if (Objects.equal(oldObjectRef, newObjectRef)) {
                // filter didn't match anything
                return Iterators.emptyIterator();
            }

            TYPE oldObjectType = oldObjectRef.isPresent() ? oldObjectRef.get().getType() : null;
            TYPE newObjectType = newObjectRef.isPresent() ? newObjectRef.get().getType() : null;

            checkState(oldObjectType == null || newObjectType == null
                    || Objects.equal(oldObjectType, newObjectType));

            final TYPE type = oldObjectType == null ? newObjectType : oldObjectType;
            switch (type) {
            case FEATURE:
                return Iterators.singletonIterator(new DiffEntry(oldObjectRef.orNull(),
                        newObjectRef.orNull()));
            case TREE:
                ObjectSerialisingFactory serialFactory = objectDb.getSerialFactory();
                if (oldObjectRef.isPresent()) {
                    oldTree = objectDb.get(oldObjectRef.get().getObjectId(),
                            serialFactory.createRevTreeReader(objectDb));
                } else {
                    oldTree = objectDb.newTree();
                }
                if (newObjectRef.isPresent()) {
                    newTree = objectDb.get(newObjectRef.get().getObjectId(),
                            serialFactory.createRevTreeReader(objectDb));
                } else {
                    newTree = objectDb.newTree();
                }
                break;
            default:
                throw new IllegalStateException(
                        "Only FEATURE or TREE obects expected at this stage: " + type);
            }
        }

        Preconditions.checkState(oldTree.isNormalized());
        Preconditions.checkState(newTree.isNormalized());

        Iterator<DiffEntry> iterator = new TreeDiffEntryIterator(oldTree, newTree, objectDb);

        if (pathFiltering) {
            iterator = Iterators.filter(iterator, new Predicate<DiffEntry>() {
                @Override
                public boolean apply(@Nullable DiffEntry input) {
                    String oldPath = input.oldPath();
                    String newPath = input.newPath();
                    boolean apply = (oldPath != null && oldPath.startsWith(pathFilter))
                            || (newPath != null && newPath.startsWith(pathFilter));
                    return apply;
                }
            });
        }
        return iterator;
    }

    private Optional<NodeRef> getFilteredObjectRef(RevTree tree) {
        checkState(!pathFilter.isEmpty());

        final DepthSearch search = new DepthSearch(objectDb, objectDb.getSerialFactory());
        Optional<NodeRef> ref = search.find(tree, pathFilter);
        return ref;
    }

    /**
     * 
     * @author groldan
     */
    private static class AddRemoveAllTreeIterator extends AbstractIterator<DiffEntry> {

        private Iterator<?> treeIterator;

        private final DiffEntry.ChangeType changeType;

        private final ObjectDatabase objectDb;

        public AddRemoveAllTreeIterator(final DiffEntry.ChangeType changeType, final RevTree tree,
                final ObjectDatabase db) {

            this(changeType, tree.iterator(null), db);
        }

        public AddRemoveAllTreeIterator(final DiffEntry.ChangeType changeType,
                final Iterator<NodeRef> treeIterator, final ObjectDatabase db) {

            this.treeIterator = treeIterator;
            this.changeType = changeType;
            this.objectDb = db;
        }

        @Override
        protected DiffEntry computeNext() {
            if (!treeIterator.hasNext()) {
                return endOfData();
            }

            final Object nextObj = treeIterator.next();
            if (nextObj instanceof DiffEntry) {
                return (DiffEntry) nextObj;
            }

            Preconditions.checkState(nextObj instanceof NodeRef);

            final NodeRef next = (NodeRef) nextObj;

            if (TYPE.TREE.equals(next.getType())) {
                RevTree tree;

                ObjectSerialisingFactory serialFactory = objectDb.getSerialFactory();
                tree = objectDb
                        .get(next.getObjectId(), serialFactory.createRevTreeReader(objectDb));

                Iterator<?> childTreeIterator;
                childTreeIterator = new AddRemoveAllTreeIterator(this.changeType, tree, objectDb);
                this.treeIterator = Iterators.concat(childTreeIterator, this.treeIterator);
                return computeNext();
            }

            Preconditions.checkState(TYPE.FEATURE.equals(next.getType()));

            NodeRef oldObject = null;
            NodeRef newObject = null;
            if (changeType == ChangeType.ADDED) {
                newObject = next;
            } else {
                oldObject = next;
            }
            DiffEntry diffEntry;
            diffEntry = new DiffEntry(oldObject, newObject);
            return diffEntry;
        }

    }

    /**
     * Traverses the direct children iterators of both trees (fromTree and toTree) simultaneously.
     * If the current children is named the same for both iterators, finds out whether the two
     * children are changed. If the two elements of the current iteration are not the same, find out
     * whether it's an addition or a deletion.
     * 
     * @author groldan
     * 
     */
    private static class TreeDiffEntryIterator extends AbstractIterator<DiffEntry> {

        private final RevTree oldTree;

        private final RevTree newTree;

        private Iterator<DiffEntry> currSubTree;

        private RewindableIterator<NodeRef> oldEntries;

        private RewindableIterator<NodeRef> newEntries;

        private final ObjectDatabase objectDb;

        public TreeDiffEntryIterator(final RevTree fromTree, final RevTree toTree,
                final ObjectDatabase db) {
            this.oldTree = fromTree;
            this.newTree = toTree;
            this.objectDb = db;
            this.oldEntries = new RewindableIterator<NodeRef>(oldTree.iterator(null));
            this.newEntries = new RewindableIterator<NodeRef>(newTree.iterator(null));
        }

        @Override
        protected DiffEntry computeNext() {
            if (currSubTree != null && currSubTree.hasNext()) {
                return currSubTree.next();
            }
            if (!oldEntries.hasNext() && !newEntries.hasNext()) {
                return endOfData();
            }
            if (oldEntries.hasNext() && !newEntries.hasNext()) {
                currSubTree = new AddRemoveAllTreeIterator(ChangeType.REMOVED, this.oldEntries,
                        this.objectDb);
                return computeNext();
            }
            if (!oldEntries.hasNext() && newEntries.hasNext()) {
                currSubTree = new AddRemoveAllTreeIterator(ChangeType.ADDED, newEntries, objectDb);
                return computeNext();
            }
            Preconditions.checkState(currSubTree == null || !currSubTree.hasNext());
            Preconditions.checkState(oldEntries.hasNext() && newEntries.hasNext());
            NodeRef nextOld = oldEntries.next();
            NodeRef nextNew = newEntries.next();

            while (nextOld.equals(nextNew)) {
                // no change, keep going, but avoid too much recursion
                if (oldEntries.hasNext() && newEntries.hasNext()) {
                    nextOld = oldEntries.next();
                    nextNew = newEntries.next();
                } else {
                    return computeNext();
                }
            }

            final String oldEntryName = nextOld.getPath();
            final String newEntryName = nextNew.getPath();

            final ChangeType changeType;
            final NodeRef oldRef, newRef;
            final RevObject.TYPE objectType;

            if (oldEntryName.equals(newEntryName)) {
                // same child name, found a changed object
                changeType = ChangeType.MODIFIED;
                objectType = nextOld.getType();
                oldRef = nextOld;
                newRef = nextNew;

            } else {
                // not the same object (blob or tree), find out whether it's an addition or a
                // deletion. Uses the same ordering than RevTree's iteration order to perform the
                // comparison
                final int comparison = ObjectId.forString(oldEntryName).compareTo(
                        ObjectId.forString(newEntryName));
                Preconditions.checkState(comparison != 0,
                        "Comparison can't be 0 if reached this point!");

                if (comparison < 0) {
                    // something was deleted in oldVersion, return a delete diff from oldVersion and
                    // return the item to the "newVersion" iterator for the next round of
                    // pair-to-pair comparisons
                    newEntries.returnElement(nextNew);
                    changeType = ChangeType.REMOVED;
                    objectType = nextOld.getType();
                    oldRef = nextOld;
                    newRef = null;
                } else {
                    // something was added in newVersion, return an "add diff" for newVersion and
                    // return the item to the "oldVersion" iterator for the next rounds of
                    // pair-to-pair comparisons
                    oldEntries.returnElement(nextOld);
                    changeType = ChangeType.ADDED;
                    objectType = nextNew.getType();
                    oldRef = null;
                    newRef = nextNew;
                }

            }

            if (RevObject.TYPE.FEATURE.equals(objectType)) {
                DiffEntry singleChange = new DiffEntry(oldRef, newRef);
                return singleChange;
            }

            Preconditions.checkState(RevObject.TYPE.TREE.equals(objectType));

            Iterator<DiffEntry> changesIterator;

            try {
                ObjectSerialisingFactory serialFactory = objectDb.getSerialFactory();
                switch (changeType) {
                case ADDED:
                case REMOVED: {
                    ObjectId treeId = null == oldRef ? newRef.getObjectId() : oldRef.getObjectId();
                    RevTree childTree = objectDb.get(treeId,
                            serialFactory.createRevTreeReader(objectDb));
                    changesIterator = new AddRemoveAllTreeIterator(changeType, childTree, objectDb);
                    break;
                }
                case MODIFIED: {
                    Preconditions.checkState(RevObject.TYPE.TREE.equals(nextOld.getType()));
                    Preconditions.checkState(RevObject.TYPE.TREE.equals(nextNew.getType()));
                    RevTree oldChildTree = objectDb.get(oldRef.getObjectId(),
                            serialFactory.createRevTreeReader(objectDb));
                    RevTree newChildTree = objectDb.get(newRef.getObjectId(),
                            serialFactory.createRevTreeReader(objectDb));
                    changesIterator = new TreeDiffEntryIterator(oldChildTree, newChildTree,
                            objectDb);
                    break;
                }
                default:
                    throw new IllegalStateException("Unrecognized change type: " + changeType);
                }
                if (this.currSubTree == null || !this.currSubTree.hasNext()) {
                    this.currSubTree = changesIterator;
                } else {
                    this.currSubTree = Iterators.concat(changesIterator, this.currSubTree);
                }
                return computeNext();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

    }

    private static class RewindableIterator<T> extends AbstractIterator<T> {

        private Iterator<T> subject;

        private LinkedList<T> returnQueue;

        public RewindableIterator(Iterator<T> subject) {
            this.subject = subject;
            this.returnQueue = new LinkedList<T>();
        }

        public void returnElement(T element) {
            this.returnQueue.offer(element);
        }

        @Override
        protected T computeNext() {
            T peak = returnQueue.poll();
            if (peak != null) {
                return peak;
            }
            if (!subject.hasNext()) {
                return endOfData();
            }
            return subject.next();
        }

    }

}
