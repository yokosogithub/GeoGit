/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTreeBuilder;
import org.geogit.storage.NodeStorageOrder;
import org.geogit.storage.ObjectDatabase;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.vividsolutions.jts.geom.Envelope;

public class RevTreeBuilderPerformanceTest extends RepositoryTestCase {

    private ObjectDatabase odb;

    private static final ObjectId FAKE_ID = ObjectId.forString("fake");

    private static final int numNodes = 512*32;

    private static Iterable<Node> nodes;

    // private static List<Node> sortedNodes;

    @BeforeClass
    public static void beforeClass() {
        // nodes = createNodes(numNodes);
        nodes = new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
                return new AbstractIterator<Node>() {
                    int count = 0;

                    @Override
                    protected Node computeNext() {
                        count++;
                        if (count > numNodes) {
                            return endOfData();
                        }
                        return createNode(count);
                    }
                };
            }
        };
        // sortedNodes = new ArrayList<Node>(nodes);
        //
        // System.err.printf("Sorting %d nodes...", numNodes);
        // Stopwatch s = new Stopwatch().start();
        // Collections.sort(sortedNodes, new NodeStorageOrder());
        // s.stop();
        // System.err.printf("done in %s\n", s);
    }

    @Override
    protected void setUpInternal() throws Exception {
        odb = repo.getObjectDatabase();
    }

    @Test
    public void testInsertUnordered() {
        System.err.println("testInsertUnordered...");
        createTree(nodes, new RevTreeBuilder(odb), true);
        // createTree(nodes, new RevTreeBuilder(odb), true);
    }

    // @Test
    // public void testInsertOrdered() {
    // System.err.println("testInsertOrdered...");
    // createTree(sortedNodes, new RevTreeBuilder(odb), true);
    // // createTree(sortedNodes, new RevTreeBuilder(odb), true);
    // }

    @Test
    public void testInsertOrderedPartitioned10K() {
        System.err.println("testInsertOrderedPartitioned10K...");
        final int partitionSize = 10 * 1000;
        testInsertPartitioned(partitionSize);
    }

    @Test
    public void testInsertOrderedPartitioned50K() {
        System.err.println("testInsertOrderedPartitioned50K...");
        final int partitionSize = 50 * 1000;
        testInsertPartitioned(partitionSize);
    }

    @Test
    public void testInsertOrderedPartitioned100K() {
        System.err.println("testInsertOrderedPartitioned100K...");
        final int partitionSize = 100 * 1000;
        testInsertPartitioned(partitionSize);
    }

    @Test
    public void testInsertOrderedPartitioned500K() {
        System.err.println("testInsertOrderedPartitioned500K...");
        final int partitionSize = 500 * 1000;
        testInsertPartitioned(partitionSize);
    }

    private void testInsertPartitioned(int partitionSize) {
        UnmodifiableIterator<List<Node>> partitions = Iterators.partition(nodes.iterator(),
                partitionSize);

        RevTreeBuilder builder = new RevTreeBuilder(odb);
        Stopwatch sw = new Stopwatch().start();
        while (partitions.hasNext()) {
            List<Node> partition = new ArrayList<Node>(partitions.next());
            Collections.sort(partition, new NodeStorageOrder());
            createTree(partition, builder, false);
        }
        System.err.println("Calling RevTreeBuilder.build()...");
        builder.build();
        sw.stop();
        System.err.printf("-- Created tree with %d sorted partitioned size in %s\n", partitionSize,
                sw);
    }

    private static List<Node> createNodes(int numNodes) {
        System.err.printf("Creating %d nodes...", numNodes);

        List<Node> nodes = new ArrayList<Node>(numNodes);

        for (int i = 0; i < numNodes; i++) {
            Node ref = createNode(i);
            nodes.add(ref);
        }
        System.err.println("done.");
        return nodes;
    }

    private static Node createNode(int i) {
        byte[] rawID = FAKE_ID.getRawValue();
        String key = "Feature." + i;
        ObjectId id = new ObjectId(rawID);
        Envelope env = new Envelope(0, 0, i, i);
        Node ref = Node.create(key, id, FAKE_ID, TYPE.FEATURE, env);
        return ref;
    }

    private RevTreeBuilder createTree(final Iterable<Node> nodes, final RevTreeBuilder b,
            final boolean buildTree) {
        if (buildTree) {
            System.err.printf("Creating treee with %d nodes...", numNodes);
        }
        Stopwatch sw = new Stopwatch().start();
        for (Node n : nodes) {
            b.put(n);
        }
        sw.stop();
        if (buildTree) {
            System.err.printf("Created in %s\n", sw);
        }
        return b;
    }

}
