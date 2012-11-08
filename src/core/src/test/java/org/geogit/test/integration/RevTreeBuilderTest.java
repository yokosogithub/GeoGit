/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.diff.DepthTreeIterator;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

public class RevTreeBuilderTest extends RepositoryTestCase {

    private ObjectDatabase odb;

    private ObjectSerialisingFactory serialFactory;

    @Override
    protected void setUpInternal() throws Exception {
        odb = repo.getObjectDatabase();
        serialFactory = repo.getSerializationFactory();
    }

    @Test
    public void testPutIterate() throws Exception {
        final int numEntries = 1000 * 100;
        ObjectId treeId;

        Stopwatch sw;
        sw = new Stopwatch().start();
        treeId = createAndSaveTree(numEntries, true);
        sw.stop();
        System.err.println("Stored " + numEntries + " tree entries in " + sw + " ("
                + Math.round(numEntries / (sw.elapsedMillis() / 1000D)) + "/s)");

        sw = new Stopwatch().start();
        treeId = createAndSaveTree(numEntries, true);
        sw.stop();
        System.err.println("Stored " + numEntries + " tree entries in " + sw + " ("
                + Math.round(numEntries / (sw.elapsedMillis() / 1000D)) + "/s)");

        sw.reset().start();
        final RevTree tree = odb.get(treeId, serialFactory.createRevTreeReader());
        sw.stop();
        System.err.println("Retrieved tree in " + sw);

        System.err.println("traversing with DepthTreeIterator...");
        sw.reset().start();
        int counted = 0;
        for (DepthTreeIterator it = new DepthTreeIterator(tree, odb, serialFactory); it.hasNext(); counted++) {
            NodeRef ref = it.next();
            if ((counted + 1) % (numEntries / 10) == 0) {
                System.err.print("#" + (counted + 1));
            } else if ((counted + 1) % (numEntries / 100) == 0) {
                System.err.print('.');
            }
        }
        sw.stop();
        System.err.println("\nTraversed " + counted + " in " + sw + " ("
                + Math.round(counted / (sw.elapsedMillis() / 1000D)) + "/s)\n");

        System.err.println("traversing with DepthTreeIterator...");
        sw.reset().start();
        counted = 0;
        for (DepthTreeIterator it = new DepthTreeIterator(tree, odb, serialFactory); it.hasNext(); counted++) {
            NodeRef ref = it.next();
            if ((counted + 1) % (numEntries / 10) == 0) {
                System.err.print("#" + (counted + 1));
            } else if ((counted + 1) % (numEntries / 100) == 0) {
                System.err.print('.');
            }
        }
        sw.stop();
        System.err.println("\nTraversed " + counted + " in " + sw + " ("
                + Math.round(counted / (sw.elapsedMillis() / 1000D)) + "/s)\n");
        assertEquals(numEntries, counted);
    }

    @Test
    public void testPutRandomGet() throws Exception {
        final int numEntries = 2 * RevTreeBuilder.DEFAULT_SPLIT_FACTOR + 1500;
        final ObjectId treeId;

        Stopwatch sw;
        sw = new Stopwatch().start();
        treeId = createAndSaveTree(numEntries, true);
        sw.stop();
        System.err.println("Stored " + numEntries + " tree entries in " + sw + " ("
                + Math.round(numEntries / (sw.elapsedMillis() / 1000D)) + "/s)");

        sw.reset().start();
        final RevTree tree = odb.get(treeId, serialFactory.createRevTreeReader());
        sw.stop();
        System.err.println("Retrieved tree in " + sw);

        {
            Map<Integer, NodeRef> randomEdits = Maps.newHashMap();
            Random randGen = new Random();
            for (int i = 0; i < 1000 * 10; i++) {
                int random;
                while (randomEdits.containsKey(random = randGen.nextInt(numEntries))) {
                    ;
                }
                String path = "Feature." + random;
                ObjectId newid = ObjectId.forString(path + "changed");
                NodeRef ref = new NodeRef(path, newid, ObjectId.NULL, TYPE.FEATURE);
                randomEdits.put(random, ref);
            }
            RevTreeBuilder mutable = tree.builder(odb);
            sw.reset().start();
            for (NodeRef ref : randomEdits.values()) {
                mutable.put(ref);
            }
            mutable.build();
            sw.stop();
            System.err.println(randomEdits.size() + " random modifications in " + sw);
        }

        // CharSequence treeStr =
        // repo.command(CatObject.class).setObject(Suppliers.ofInstance(tree))
        // .call();
        // System.out.println(treeStr);

        final FindTreeChild childFinder = repo.command(FindTreeChild.class).setParent(tree);

        sw.reset().start();
        System.err.println("Reading " + numEntries + " entries....");
        for (int i = 0; i < numEntries; i++) {
            if ((i + 1) % (numEntries / 10) == 0) {
                System.err.print("#" + (i + 1));
            } else if ((i + 1) % (numEntries / 100) == 0) {
                System.err.print('.');
            }
            String key = "Feature." + i;
            // ObjectId oid = ObjectId.forString(key);
            Optional<NodeRef> ref = childFinder.setChildPath(key).call();
            assertTrue(key, ref.isPresent());
            // assertEquals(key, ref.get().getPath());
            // assertEquals(key, oid, ref.get().getObjectId());
        }
        sw.stop();
        System.err.println("\nGot " + numEntries + " in " + sw.elapsedMillis() + "ms ("
                + Math.round(numEntries / (sw.elapsedMillis() / 1000D)) + "/s)\n");

    }

    @Test
    public void testRemove() throws Exception {
        final int numEntries = 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        final RevTree tree = odb.get(treeId, serialFactory.createRevTreeReader());

        // collect some keys to remove
        final Set<String> removedKeys = new HashSet<String>();
        {
            int i = 0;
            DepthTreeIterator it = new DepthTreeIterator(tree, odb, serialFactory);
            for (; it.hasNext(); i++) {
                NodeRef entry = it.next();
                if (i % 10 == 0) {
                    removedKeys.add(entry.getPath());
                }
            }
            assertEquals(100, removedKeys.size());
        }

        final RevTreeBuilder builder = tree.builder(odb);
        for (String key : removedKeys) {
            assertTrue(builder.get(key).isPresent());
            builder.remove(key);
            assertFalse(builder.get(key).isPresent());
        }

        final RevTree tree2 = builder.build();

        for (String key : removedKeys) {
            assertFalse(repo.getTreeChild(tree2, key).isPresent());
        }
    }

    @Test
    public void testRemoveSplittedTree() throws Exception {
        final int numEntries = (int) (1.5 * RevTreeBuilder.DEFAULT_SPLIT_FACTOR);
        final ObjectId treeId = createAndSaveTree(numEntries, true);
        final RevTree tree = odb.get(treeId, serialFactory.createRevTreeReader());

        // collect some keys to remove
        final Set<String> removedKeys = new HashSet<String>();
        {
            int i = 0;
            DepthTreeIterator it = new DepthTreeIterator(tree, odb, serialFactory);
            for (; it.hasNext(); i++) {
                NodeRef entry = it.next();
                if (i % 10 == 0) {
                    removedKeys.add(entry.getPath());
                }
            }
            assertTrue(removedKeys.size() > 0);
        }

        RevTreeBuilder builder = tree.builder(odb);
        for (String key : removedKeys) {
            assertTrue(key, builder.get(key).isPresent());
            builder.remove(key);
            assertFalse(key, builder.get(key).isPresent());
        }

        for (String key : removedKeys) {
            assertFalse(builder.get(key).isPresent());
        }

        final RevTree tree2 = builder.build();

        for (String key : removedKeys) {
            assertFalse(key, repo.getTreeChild(tree2, key).isPresent());
        }
    }

    /**
     * Assert two trees that have the same contents resolve to the same id regardless of the order
     * the contents were added
     * 
     * @throws Exception
     */
    @Test
    public void testEquality() throws Exception {
        testEquality(100);
        testEquality(100 + RevTreeBuilder.DEFAULT_SPLIT_FACTOR);
    }

    private void testEquality(final int numEntries) throws Exception {
        final ObjectId treeId1;
        final ObjectId treeId2;
        treeId1 = createAndSaveTree(numEntries, true);
        treeId2 = createAndSaveTree(numEntries, false);

        assertEquals(treeId1, treeId2);
    }

    private ObjectId createAndSaveTree(final int numEntries, final boolean insertInAscendingKeyOrder)
            throws Exception {

        RevTreeBuilder treeBuilder = createTree(numEntries, insertInAscendingKeyOrder);
        RevTree tree = treeBuilder.build();
        odb.put(tree.getId(), serialFactory.createRevTreeWriter(tree));
        return tree.getId();
    }

    private RevTreeBuilder createTree(final int numEntries, final boolean insertInAscendingKeyOrder) {
        RevTreeBuilder tree = new RevTreeBuilder(odb, serialFactory);

        final int increment = insertInAscendingKeyOrder ? 1 : -1;
        final int from = insertInAscendingKeyOrder ? 0 : numEntries - 1;
        final int breakAt = insertInAscendingKeyOrder ? numEntries : -1;

        int c = 0;
        for (int i = from; i != breakAt; i += increment, c++) {
            addNodeRef(tree, i);
            if ((c + 1) % (numEntries / 10) == 0) {
                System.err.print("#" + (c + 1));
            } else if ((c + 1) % (numEntries / 100) == 0) {
                System.err.print('.');
            }
        }
        System.err.print('\n');
        return tree;
    }

    private static final ObjectId FAKE_ID = ObjectId.forString("fake");

    private void addNodeRef(RevTreeBuilder tree, int i) {
        String key = "Feature." + i;
        // ObjectId oid = ObjectId.forString(key);
        // ObjectId metadataId = ObjectId.forString("FeatureType");
        // NodeRef ref = new NodeRef(key, oid, metadataId, TYPE.FEATURE);
        NodeRef ref = new NodeRef(key, FAKE_ID, FAKE_ID, TYPE.FEATURE);
        tree.put(ref);
    }
}
