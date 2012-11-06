/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.geogit.api.NodeRef;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
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

    private ObjectSerialisingFactory serialFactory;

    public DiffTreeWalk(final ObjectDatabase db, final RevTree fromRootTree,
            final RevTree toRootTree, final ObjectSerialisingFactory serialFactory) {
        Preconditions.checkNotNull(db);
        Preconditions.checkNotNull(fromRootTree);
        Preconditions.checkNotNull(toRootTree);
        Preconditions.checkNotNull(serialFactory);
        this.objectDb = db;
        this.fromRootTree = fromRootTree;
        this.toRootTree = toRootTree;
        this.serialFactory = serialFactory;
        this.pathFilter = "";// root
    }

    public void setFilter(final String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isEmpty()) {
            this.pathFilter = "";// root
        } else {
            this.pathFilter = pathPrefix;
        }
    }

    public Iterator<DiffEntry> get() {

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
                if (oldObjectRef.isPresent()) {
                    oldTree = objectDb.get(oldObjectRef.get().getObjectId(),
                            serialFactory.createRevTreeReader());
                } else {
                    oldTree = RevTree.EMPTY;
                }
                if (newObjectRef.isPresent()) {
                    newTree = objectDb.get(newObjectRef.get().getObjectId(),
                            serialFactory.createRevTreeReader());
                } else {
                    newTree = RevTree.EMPTY;
                }
                break;
            default:
                throw new IllegalStateException(
                        "Only FEATURE or TREE obects expected at this stage: " + type);
            }
        }

        // TODO: pass pathFilter to TreeDiffEntryIterator so it ignores inner trees where the path
        // is guaranteed not to be present
        Iterator<DiffEntry> iterator = new TreeDiffEntryIterator(oldTree, newTree, objectDb,
                serialFactory);

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

        final DepthSearch search = new DepthSearch(objectDb, serialFactory);
        Optional<NodeRef> ref = search.find(tree, pathFilter);
        return ref;
    }

}
