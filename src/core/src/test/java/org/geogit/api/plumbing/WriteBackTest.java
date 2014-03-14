/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.MemoryModule;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.di.GeogitModule;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

/**
 *
 */
public class WriteBackTest extends Assert {

    private WriteBack writeBack;

    ObjectDatabase odb;

    StagingDatabase indexDb;

    @Before
    public void setUp() {
        Injector injector = Guice.createInjector(Modules.override(new GeogitModule()).with(
                new MemoryModule(null)));

        odb = injector.getInstance(ObjectDatabase.class);
        indexDb = injector.getInstance(StagingDatabase.class);
        odb.open();
        indexDb.open();

        writeBack = injector.getInstance(WriteBack.class);
    }

    @Test
    public void testSimple() {

        RevTreeBuilder oldRoot = new RevTreeBuilder(odb);
        RevTree tree = new RevTreeBuilder(odb).put(blob("blob")).build();
        ObjectId newRootId = writeBack.setAncestor(oldRoot).setChildPath("subtree").setTree(tree)
                .call();

        Optional<NodeRef> ref = new DepthSearch(odb).find(newRootId, "subtree");
        assertTrue(ref.isPresent());
    }

    @Test
    public void testSingleLevel() {

        RevTreeBuilder oldRoot = new RevTreeBuilder(odb);

        RevTree tree = new RevTreeBuilder(odb).put(blob("blob")).build();

        ObjectId newRootId = writeBack.setAncestor(oldRoot).setChildPath("level1").setTree(tree)
                .call();

        // created the intermediate tree node?
        Optional<NodeRef> ref;
        DepthSearch depthSearch = new DepthSearch(odb);
        ref = depthSearch.find(newRootId, "level1");
        assertTrue(ref.isPresent());

        ref = depthSearch.find(newRootId, "level1/blob");
        assertTrue(ref.isPresent());
    }

    @Test
    public void testSingleNested() {

        RevTreeBuilder oldRoot = new RevTreeBuilder(odb);

        RevTree tree = new RevTreeBuilder(odb).put(blob("blob")).build();

        ObjectId newRootId = writeBack.setAncestor(oldRoot).setChildPath("level1/level2")
                .setTree(tree).call();

        // created the intermediate tree node?
        Optional<NodeRef> ref;
        DepthSearch depthSearch = new DepthSearch(odb);
        ref = depthSearch.find(newRootId, "level1");
        assertTrue(ref.isPresent());

        ref = depthSearch.find(newRootId, "level1/level2");
        assertTrue(ref.isPresent());

        ref = depthSearch.find(newRootId, "level1/level2/blob");
        assertTrue(ref.isPresent());
    }

    @Test
    public void testSiblingsSingleLevel() {

        RevTreeBuilder ancestor = new RevTreeBuilder(odb);

        RevTree tree1 = new RevTreeBuilder(odb).put(blob("blob")).build();
        RevTree tree2 = new RevTreeBuilder(odb).put(blob("blob")).build();

        ObjectId newRootId1 = writeBack.setAncestor(ancestor).setChildPath("subtree1")
                .setTree(tree1).call();

        ancestor = odb.getTree(newRootId1).builder(odb);
        ObjectId newRootId2 = writeBack.setAncestor(ancestor).setChildPath("subtree2")
                .setTree(tree2).call();

        // created the intermediate tree node?
        DepthSearch depthSearch = new DepthSearch(odb);
        assertTrue(depthSearch.find(newRootId2, "subtree1").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree2").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree1/blob").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree2/blob").isPresent());
    }

    @Test
    public void testSiblingsNested() {

        RevTreeBuilder oldRoot = new RevTreeBuilder(odb);

        RevTree tree1 = new RevTreeBuilder(odb).put(blob("blob")).build();
        RevTree tree2 = new RevTreeBuilder(odb).put(blob("blob")).build();

        ObjectId newRootId1 = writeBack.setAncestor(oldRoot).setChildPath("subtree1/level2")
                .setTree(tree1).call();

        ObjectId newRootId2 = writeBack.setAncestor(odb.getTree(newRootId1).builder(odb))
                .setChildPath("subtree2/level2/level3").setTree(tree2).call();

        // created the intermediate tree node?
        DepthSearch depthSearch = new DepthSearch(odb);
        assertTrue(depthSearch.find(newRootId2, "subtree1").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree1/level2").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree1/level2/blob").isPresent());

        assertTrue(depthSearch.find(newRootId2, "subtree2").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree2/level2").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree2/level2/level3").isPresent());
        assertTrue(depthSearch.find(newRootId2, "subtree2/level2/level3/blob").isPresent());
    }

    @Test
    public void testPreserveMetadataId() {

        RevTreeBuilder oldRoot = new RevTreeBuilder(odb);

        RevTree tree = new RevTreeBuilder(odb).put(blob("blob")).build();

        final ObjectId treeMetadataId = ObjectId.forString("fakeMdId");

        ObjectId newRootId = writeBack.setAncestor(oldRoot).setChildPath("level1/level2")
                .setTree(tree).setMetadataId(treeMetadataId).call();

        Optional<NodeRef> ref;
        DepthSearch depthSearch = new DepthSearch(odb);
        ref = depthSearch.find(newRootId, "level1/level2");
        assertTrue(ref.isPresent());
        assertTrue(ref.get().getNode().getMetadataId().isPresent());
        assertFalse(ref.get().getNode().getMetadataId().get().isNull());
        assertEquals(treeMetadataId, ref.get().getNode().getMetadataId().get());
    }

    private Node blob(String path) {
        return Node.create(path, ObjectId.forString(path), ObjectId.NULL, TYPE.FEATURE, null);
    }
}
