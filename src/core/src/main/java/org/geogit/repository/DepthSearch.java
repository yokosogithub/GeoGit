/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.LinkedList;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

public class DepthSearch {

    private final ObjectDatabase objectDb;

    private ObjectSerialisingFactory serialFactory;

    public DepthSearch(final ObjectDatabase db, ObjectSerialisingFactory serialFactory) {
        this.objectDb = db;
        this.serialFactory = serialFactory;
    }

    public NodeRef find(final ObjectId treeId, final List<String> path) {
        RevTree tree = objectDb.get(treeId, serialFactory.createRevTreeReader(objectDb));
        if (tree == null) {
            return null;
        }
        return find(tree, path);
    }

    public NodeRef find(final RevTree tree, final List<String> path) {
        if (path.size() == 1) {
            return tree.get(path.get(0));
        }
        final String childName = path.get(0);
        final NodeRef childTreeRef = tree.get(childName);
        if (childTreeRef == null) {
            return null;
        }
        final RevTree childTree = objectDb.get(childTreeRef.getObjectId(),
                serialFactory.createRevTreeReader(objectDb));
        final List<String> subpath = path.subList(1, path.size());
        return find(childTree, subpath);
    }

    /**
     * @param tree
     * @param childId
     * @return the tuple of tree path/object reference if found, or {@code null} if not.
     */
    public Tuple<List<String>, NodeRef> find(final RevTree tree, final ObjectId childId) {
        final NodeRef[] target = new NodeRef[1];
        final List<String> path = new LinkedList<String>();

        class IdFindedVisitor implements TreeVisitor {

            @Override
            public boolean visitEntry(NodeRef ref) {
                if (childId.equals(ref.getObjectId())) {
                    target[0] = ref;
                    path.add(ref.getName());
                    return false;// end walk
                }
                if (TYPE.TREE.equals(ref.getType())) {
                    final int idx = path.size();
                    path.add(ref.getName());
                    objectDb.get(ref.getObjectId(), serialFactory.createRevTreeReader(objectDb))
                            .accept(this);
                    if (target[0] == null) {
                        path.remove(idx);
                    } else {
                        // found, stop walk
                        return false;
                    }
                }
                return true;// continue
            }

            @Override
            public boolean visitSubTree(int bucket, ObjectId treeId) {
                return true;
            }
        }

        tree.accept(new IdFindedVisitor());
        if (target[0] == null) {
            return null;
        }
        return new Tuple<List<String>, NodeRef>(path, target[0]);
    }
}
