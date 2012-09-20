/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.custommonkey.xmlunit.XMLAssert;
import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.RevSHA1Tree;
import org.geogit.test.integration.PrintTreeVisitor;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;

public class RevSHA1TreeTest extends RepositoryTestCase {

    private ObjectDatabase odb;

    @Override
    protected void setUpInternal() throws Exception {
        odb = repo.getObjectDatabase();
    }

    @Test
    public void testPutGet() throws Exception {

        final int numEntries = 1000 * 10;
        final ObjectId treeId;

        Stopwatch sw;
        sw = new Stopwatch();
        sw.start();
        treeId = createAndSaveTree(numEntries, true);
        sw.stop();

        System.err.println("\n" + sw.toString());
        System.err.println("... at " + (numEntries / ((double) sw.elapsedMillis() / 1000L)) + "/s");

        // System.err.println("\nPut " + numEntries + " in " + sw.getLastTaskTimeMillis() + "ms ("
        // + (numEntries / sw.getTotalTimeSeconds()) + "/s)");

        sw.reset().start();
        RevTree tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));
        sw.stop();
        System.out.println("Retrieved tree in " + sw);

        sw = new Stopwatch();
        sw.start();
        PrintWriter writer = new PrintWriter(System.err);
        PrintTreeVisitor visitor = new PrintTreeVisitor(writer, repo);
        tree.accept(visitor);
        writer.flush();
        sw.stop();
        System.err.println("\nTraversed " + numEntries + " in " + sw.elapsedMillis() + "ms ("
                + (numEntries / ((double) sw.elapsedMillis() / 1000L)) + "/s)\n");
        assertEquals(numEntries, visitor.visitedEntries);

        tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));
        sw = new Stopwatch();
        sw.start();
        System.err.println("Reading " + numEntries + " entries....");
        for (int i = 0; i < numEntries; i++) {
            if ((i + 1) % (numEntries / 10) == 0) {
                System.err.print("#" + (i + 1));
            } else if ((i + 1) % (numEntries / 100) == 0) {
                System.err.print('.');
            }
            String key = "Feature." + i;
            ObjectId oid = ObjectId.forString(key);
            Optional<NodeRef> ref = tree.get(key);
            assertTrue(ref.isPresent());
            assertEquals(key, oid, ref.get().getObjectId());
        }
        sw.stop();
        System.err.println("\nGot " + numEntries + " in " + sw.elapsedMillis() + "ms ("
                + (numEntries / ((double) sw.elapsedMillis() / 1000L)) + "/s)\n");

    }

    @Test
    public void testRemove() throws Exception {
        final int numEntries = 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        RevTree tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));

        // collect some keys to remove
        final Set<String> removedKeys = new HashSet<String>();
        tree.accept(new TreeVisitor() {
            int i = 0;

            public boolean visitSubTree(int bucket, ObjectId treeId) {
                return true;
            }

            public boolean visitEntry(final NodeRef entry) {
                if (i % 10 == 0) {
                    removedKeys.add(entry.getPath());
                }
                i++;
                return true;
            }
        });

        assertEquals(100, removedKeys.size());

        tree = tree.mutable();
        for (String key : removedKeys) {
            ((MutableTree) tree).remove(key);
        }

        for (String key : removedKeys) {
            assertFalse(tree.get(key).isPresent());
        }

        final ObjectId newTreeId = odb.put(getRepository().newRevTreeWriter(tree));
        RevTree tree2 = odb.get(newTreeId, getRepository().newRevTreeReader(odb, 0));

        for (String key : removedKeys) {
            assertFalse(tree2.get(key).isPresent());
        }
    }

    @Test
    public void testRemoveSplittedTree() throws Exception {
        final int numEntries = RevSHA1Tree.SPLIT_FACTOR + 100;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        RevTree tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));

        // collect some keys to remove
        final Set<String> removedKeys = new HashSet<String>();
        tree.accept(new TreeVisitor() {
            int i = 0;

            public boolean visitSubTree(int bucket, ObjectId treeId) {
                return true;
            }

            public boolean visitEntry(final NodeRef entry) {
                if (i % 10 == 0) {
                    removedKeys.add(entry.getPath());
                }
                i++;
                return true;
            }
        });

        assertTrue(removedKeys.size() > 0);

        tree = tree.mutable();
        for (String key : removedKeys) {
            ((MutableTree) tree).remove(key);
        }

        for (String key : removedKeys) {
            assertFalse(tree.get(key).isPresent());
        }

        final ObjectId newTreeId = odb.put(getRepository().newRevTreeWriter(tree));
        RevTree tree2 = odb.get(newTreeId, getRepository().newRevTreeReader(odb, 0));

        for (String key : removedKeys) {
            assertFalse(tree2.get(key).isPresent());
        }
    }

    @Test
    @Ignore
    public void testSize() throws Exception {
        Stopwatch sw = new Stopwatch().start();
        final int numEntries = RevSHA1Tree.SPLIT_FACTOR + 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        RevTree tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));

        int size = tree.size().intValue();
        assertEquals(numEntries, size);

        // add a couple more
        final int added = 25000;
        tree = tree.mutable();
        for (int i = numEntries; i < numEntries + added; i++) {
            addNodeRef((MutableTree) tree, i);
        }

        size = tree.size().intValue();
        assertEquals(numEntries + added, size);

        // save and compute again
        treeId = odb.put(getRepository().newRevTreeWriter(tree));
        tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));

        size = tree.size().intValue();
        assertEquals(numEntries + added, size);

        // remove some keys
        final int removed = RevSHA1Tree.SPLIT_FACTOR;
        tree = tree.mutable();
        for (int i = 1; i <= removed; i++) {
            String key = "Feature." + (size - i);
            ((MutableTree) tree).remove(key);
        }

        size = tree.size().intValue();
        assertEquals(numEntries + added - removed, tree.size().intValue());
        // save and compute again
        treeId = odb.put(getRepository().newRevTreeWriter(tree));
        tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));
        size = tree.size().intValue();
        assertEquals(numEntries + added - removed, tree.size().intValue());

        // replacing an existing key should not change size
        tree = tree.mutable();

        final ObjectId fakeMetadataId = ObjectId.forString("FeatureType");

        for (int i = 0; i < size / 2; i += 2) {
            String key = "Feature." + i;
            ObjectId otherId = ObjectId.forString(key + "changed");
            ((MutableTree) tree).put(new NodeRef(key, otherId, fakeMetadataId, TYPE.FEATURE));
        }
        final int expected = size;
        size = tree.size().intValue();
        assertEquals(expected, tree.size().intValue());
        // save and compute again
        treeId = odb.put(getRepository().newRevTreeWriter(tree));
        tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));
        size = tree.size().intValue();
        assertEquals(expected, tree.size().intValue());
        sw.stop();
        System.err.println("testSize run time: " + sw);
    }

    @Test
    public void testIterator() throws Exception {
        final int numEntries = RevSHA1Tree.SPLIT_FACTOR + 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        RevTree tree = odb.get(treeId, getRepository().newRevTreeReader(odb, 0));

        Iterator<NodeRef> iterator = tree.iterator(null);
        assertNotNull(iterator);
        int count = 0;
        while (iterator.hasNext()) {
            assertNotNull(iterator.next());
            count++;
        }
        assertEquals(numEntries, count);
    }

    @Test
    public void testPrint() throws Exception {
        final int numEntries = RevSHA1Tree.SPLIT_FACTOR + 1000;
        ObjectId treeId = createAndSaveTree(numEntries, true);
        InputStream in = odb.getRaw(treeId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getRepository().newBlobPrinter().print(in, System.out);

        in = odb.getRaw(treeId);
        getRepository().newBlobPrinter().print(in, new PrintStream(out));

        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(dom);
        XMLAssert.assertXpathExists("/tree/tree/bucket", dom);
        XMLAssert.assertXpathExists("/tree/tree/objectid", dom);
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
        testEquality(100 + RevSHA1Tree.SPLIT_FACTOR);
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
        final ObjectId treeId;

        RevTree tree = createTree(numEntries, insertInAscendingKeyOrder);
        treeId = odb.put(getRepository().newRevTreeWriter(tree));
        return treeId;
    }

    private RevTree createTree(final int numEntries, final boolean insertInAscendingKeyOrder) {
        MutableTree tree = new RevSHA1Tree(odb).mutable();

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

    private void addNodeRef(MutableTree tree, int i) {
        String key = "Feature." + i;
        ObjectId oid = ObjectId.forString(key);
        ObjectId metadataId = ObjectId.forString("FeatureType");
        tree.put(new NodeRef(key, oid, metadataId, TYPE.FEATURE));
    }

    public static void main(String[] argv) {
        RevSHA1TreeTest test = new RevSHA1TreeTest();
        try {
            test.setUp();
            try {
                test.testPutGet();
            } finally {
                test.tearDown();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }
}
