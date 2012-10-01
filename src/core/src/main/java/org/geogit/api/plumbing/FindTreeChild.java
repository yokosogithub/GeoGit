/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;

/**
 * @see ResolveTreeish
 * @see RevObjectParse
 */
public class FindTreeChild extends AbstractGeoGitOp<Optional<NodeRef>> {

    private Supplier<RevTree> parent;

    private String childPath;

    private ObjectSerialisingFactory serialFactory;

    private String parentPath;

    private boolean indexDb;

    private StagingDatabase index;

    private ObjectDatabase odb;

    @Inject
    public FindTreeChild(ObjectSerialisingFactory serialFactory, ObjectDatabase odb,
            StagingDatabase index) {
        this.serialFactory = serialFactory;
        this.odb = odb;
        this.index = index;
    }

    /**
     * @param indexDb whether to look up in the {@link StagingDatabase index db} ({@code true}) or
     *        on the repository's {@link ObjectDatabase object database} (default)
     */
    public FindTreeChild setIndex(final boolean indexDb) {
        this.indexDb = indexDb;
        return this;
    }

    /**
     * @param tree a supplier that resolves to the tree where to start the search for the nested
     *        child. If not supplied the current HEAD tree is assumed.
     */
    public FindTreeChild setParent(Supplier<RevTree> tree) {
        this.parent = tree;
        return this;
    }

    public FindTreeChild setParent(RevTree tree) {
        this.parent = Suppliers.ofInstance(tree);
        return this;
    }

    /**
     * @param parentPath the parent's path. If not given parent is assumed to be a root tree.
     */
    public FindTreeChild setParentPath(String parentPath) {
        this.parentPath = parentPath;
        return this;
    }

    /**
     * @param childPath the full path of the subtree to look for
     */
    public FindTreeChild setChildPath(String childPath) {
        this.childPath = childPath;
        return this;
    }

    @Override
    public Optional<NodeRef> call() {
        checkNotNull(childPath, "childPath");
        final RevTree tree;
        if (parent == null) {
            ObjectId rootTreeId = command(ResolveTreeish.class).setTreeish(Ref.HEAD).call();
            if (rootTreeId.isNull()) {
                return Optional.absent();
            }
            tree = command(RevObjectParse.class).setObjectId(rootTreeId).call(RevTree.class);
        } else {
            tree = parent.get();
        }
        final String path = childPath;
        final String parentPath = this.parentPath == null ? "" : this.parentPath;
        final ObjectDatabase target = indexDb ? index : odb;

        DepthSearch depthSearch = new DepthSearch(target, serialFactory);
        Optional<NodeRef> childRef = depthSearch.find(tree, parentPath, path);
        return childRef;
    }

}
