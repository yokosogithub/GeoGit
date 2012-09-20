/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.repository;

import static org.geogit.api.RevObject.TYPE.FEATURE;
import static org.geogit.api.RevObject.TYPE.TREE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.hessian.HessianFactory;
import org.geogit.storage.memory.HeapObjectDatabse;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

/**
 *
 */
public class DepthSearchTest {

    private ObjectDatabase odb;

    private ObjectSerialisingFactory serialFactory;

    private DepthSearch search;

    private ObjectId rootTreeId;

    @Before
    public void setUp() {
        serialFactory = new HessianFactory();
        odb = new HeapObjectDatabse();
        ((AbstractObjectDatabase) odb).setSerialFactory(serialFactory);
        odb.create();

        search = new DepthSearch(odb, serialFactory);

        MutableTree root = odb.newTree();
        root = addTree(root, "path/to/tree1", "node11", "node12", "node13");
        root = addTree(root, "path/to/tree2", "node21", "node22", "node23");
        root = addTree(root, "tree3", "node31", "node32", "node33");
        rootTreeId = odb.put(serialFactory.createRevTreeWriter(root));
    }

    private MutableTree addTree(MutableTree root, final String treePath, String... singleNodeNames) {

        MutableTree subTree = odb.getOrCreateSubTree(root, treePath);
        if (singleNodeNames != null) {
            for (String singleNodeName : singleNodeNames) {
                String nodePath = NodeRef.appendChild(treePath, singleNodeName);
                ObjectId fakeFeatureOId = ObjectId.forString(nodePath);
                ObjectId fakeTypeOId = ObjectId.forString(treePath);
                subTree.put(new NodeRef(nodePath, fakeFeatureOId, fakeTypeOId, TYPE.FEATURE));
            }
        }
        ObjectId newRoot = odb.writeBack(root, subTree, treePath);
        return odb.get(newRoot, serialFactory.createRevTreeReader(odb)).mutable();
    }

    @Test
    public void testFindFromRoot() {
        assertNodeRef(find(rootTreeId, "path"), TREE, "path");
        assertNodeRef(find(rootTreeId, "path/to"), TREE, "path/to");
        assertNodeRef(find(rootTreeId, "path/to/tree1"), TREE, "path/to/tree1");
        assertNodeRef(find(rootTreeId, "path/to/tree1/node11"), FEATURE, "path/to/tree1/node11");
        assertNodeRef(find(rootTreeId, "path/to/tree1/node12"), FEATURE, "path/to/tree1/node12");
        assertNodeRef(find(rootTreeId, "path/to/tree1/node13"), FEATURE, "path/to/tree1/node13");
        assertFalse(find(rootTreeId, "path/to/tree1/node14").isPresent());

        assertNodeRef(find(rootTreeId, "path/to/tree2"), TREE, "path/to/tree2");
        assertNodeRef(find(rootTreeId, "path/to/tree2/node21"), FEATURE, "path/to/tree2/node21");
        assertNodeRef(find(rootTreeId, "path/to/tree2/node22"), FEATURE, "path/to/tree2/node22");
        assertNodeRef(find(rootTreeId, "path/to/tree2/node23"), FEATURE, "path/to/tree2/node23");
        assertFalse(find(rootTreeId, "path/to/tree2/node24").isPresent());

        assertNodeRef(find(rootTreeId, "tree3"), TYPE.TREE, "tree3");
        assertNodeRef(find(rootTreeId, "tree3/node31"), FEATURE, "tree3/node31");
        assertNodeRef(find(rootTreeId, "tree3/node32"), FEATURE, "tree3/node32");
        assertNodeRef(find(rootTreeId, "tree3/node33"), FEATURE, "tree3/node33");
        assertFalse(find(rootTreeId, "tree3/node34").isPresent());

        assertFalse(find(rootTreeId, "tree4").isPresent());

        try {
            find(rootTreeId, "");
            fail("expected IAE on empty child path");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("empty child path"));
        }

        try {
            find(rootTreeId, "/");
            fail("expected IAE on empty child path");
        } catch (IllegalArgumentException expected) {
            assertTrue(true);
        }
    }

    private Optional<NodeRef> find(ObjectId rootTreeId, String rootChildPath) {
        return search.find(rootTreeId, rootChildPath);
    }

    private void assertNodeRef(Optional<NodeRef> ref, TYPE type, String path) {
        assertTrue(ref.isPresent());
        assertEquals(type, ref.get().getType());
        assertEquals(path, ref.get().getPath());
    }
}
