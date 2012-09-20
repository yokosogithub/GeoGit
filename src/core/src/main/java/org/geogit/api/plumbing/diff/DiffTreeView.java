/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;

/**
 *
 */
public class DiffTreeView implements RevTree {

    private ObjectDatabase lookupDb;

    private final RevTree origTree;

    private Map<String, NodeRef> changes;

    private String treePath;

    private BigInteger adjustedSize;

    public DiffTreeView(final ObjectDatabase lookupDb, final String thisTreePath,
            final RevTree origTree, final Map<String, NodeRef> changes) {
        this.lookupDb = lookupDb;
        this.treePath = thisTreePath;
        this.origTree = origTree;
        this.adjustedSize = null;

        if (thisTreePath.isEmpty()) {
            this.changes = changes;
        } else {
            Predicate<String> keyPredicate = new Predicate<String>() {
                @Override
                public boolean apply(final String path) {
                    return NodeRef.isDirectChild(treePath, path);
                }
            };
            this.changes = Maps.filterKeys(changes, keyPredicate);
        }
    }

    @Override
    public MutableTree mutable() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TYPE getType() {
        return TYPE.TREE;
    }

    @Override
    public ObjectId getId() {
        return ObjectId.NULL;
    }

    @Override
    public Optional<NodeRef> get(String key) {
        NodeRef changed = changes.get(key);
        if (changed != null) {
            if (changed.getObjectId().equals(ObjectId.NULL)) {
                return Optional.absent();
            }
            return Optional.of(changed);
        }
        return origTree.get(key);
    }

    @Override
    public void accept(TreeVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized BigInteger size() {
        if (adjustedSize == null) {
            long size = origTree.size().longValue();
            for (NodeRef e : changes.values()) {
                if (e.getObjectId().equals(ObjectId.NULL)) {
                    size--;
                } else if (!origTree.get(e.getPath()).isPresent()) {
                    size++;
                }
            }
            adjustedSize = BigInteger.valueOf(size);
        }
        return adjustedSize;
    }

    @Override
    public Iterator<NodeRef> iterator(Predicate<NodeRef> filter) {
        Iterator<NodeRef> origIterator = origTree.iterator(filter);
        UnmodifiableIterator<NodeRef> deletesRemoved = Iterators.filter(origIterator,
                new Predicate<NodeRef>() {
                    @Override
                    public boolean apply(NodeRef orig) {
                        String path = orig.getPath();
                        NodeRef changed = changes.get(path);
                        if (changed != null && changed.getObjectId().isNull()) {
                            return false;
                        }
                        return true;
                    }
                });

        Iterator<NodeRef> changesApplied = Iterators.transform(deletesRemoved,
                new Function<NodeRef, NodeRef>() {

                    @Override
                    public NodeRef apply(NodeRef orig) {
                        String path = orig.getPath();
                        NodeRef changed = changes.get(path);
                        if (changed != null) {
                            return changed;
                        }
                        return orig;
                    }
                });

        return changesApplied;
    }

    /**
     * @return true
     */
    @Override
    public boolean isNormalized() {
        return true;
    }

}
