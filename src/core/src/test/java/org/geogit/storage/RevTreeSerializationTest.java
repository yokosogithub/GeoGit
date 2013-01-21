package org.geogit.storage;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeImpl;
import org.geogit.api.SpatialNode;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;

public abstract class RevTreeSerializationTest extends Assert {

    private ObjectSerialisingFactory factory;

    private ObjectDatabase mockDb;

    @Before
    public void setUp() throws Exception {
        mockDb = mock(ObjectDatabase.class);
        factory = getFactory();
    }

    @Test
    public void testTreeWithoutBuckets() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT / 2;
        TreeSet<Node> features = Sets.newTreeSet(new NodeStorageOrder());
        TreeSet<Node> trees = Sets.newTreeSet(new NodeStorageOrder());
        for (int i = 0; i < numChildren; i++) {
            String path = "path/feature." + Integer.toString(i);
            features.add(new Node(path, ObjectId.forString(path), ObjectId.NULL, TYPE.FEATURE));
        }
        RevTree revTree = createTree(features, trees);
        testTreeSerialization(revTree);
    }

    @Test
    public void testTreeWithBuckets() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT * 2;
        TreeSet<Node> features = Sets.newTreeSet(new NodeStorageOrder());
        TreeSet<Node> trees = Sets.newTreeSet(new NodeStorageOrder());
        for (int i = 0; i < numChildren; i++) {
            String path = "path/feature." + Integer.toString(i);
            features.add(new Node(path, ObjectId.forString(path), ObjectId.NULL, TYPE.FEATURE));
        }
        RevTree revTree = createTree(features, trees);
        testTreeSerialization(revTree);
    }

    @Test
    public void testTreeWithTrees() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT / 2;
        TreeSet<Node> features = Sets.newTreeSet(new NodeStorageOrder());
        TreeSet<Node> trees = Sets.newTreeSet(new NodeStorageOrder());
        for (int i = 0; i < numChildren; i++) {
            String path = "path" + Integer.toString(i);
            features.add(new Node(path, ObjectId.forString(path), ObjectId.NULL, TYPE.TREE));
        }
        RevTree revTree = createTree(features, trees);
        testTreeSerialization(revTree);
    }

    @Test
    public void testTreeWithSpatialNodes() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT * 2;
        TreeSet<Node> features = Sets.newTreeSet(new NodeStorageOrder());
        TreeSet<Node> trees = Sets.newTreeSet(new NodeStorageOrder());
        for (int i = 0; i < numChildren; i++) {
            String path = "path/feature." + Integer.toString(i);
            features.add(new SpatialNode(path, ObjectId.forString(path), ObjectId.NULL,
                    TYPE.FEATURE, new ReferencedEnvelope(0, 1, 0, 1, DefaultGeographicCRS.WGS84)));
        }
        RevTree revTree = createTree(features, trees);
        testTreeSerialization(revTree);
    }

    private RevTree createTree(TreeSet<Node> features, TreeSet<Node> trees) {

        ImmutableList<Node> sortedRefs = ImmutableList.copyOf(features);
        long size = sortedRefs.size();

        ObjectId id = ObjectId.forString("TREE_ID");
        ImmutableList<Node> treeRefs = ImmutableList.copyOf(trees);
        RevTreeImpl tree = RevTreeImpl.createLeafTree(id, size, sortedRefs, treeRefs);

        when(mockDb.getTree(eq(id))).thenReturn(tree);

        return tree;
    }

    private void testTreeSerialization(RevTree revTree) throws Exception {
        ObjectWriter<RevObject> writer = factory.createObjectWriter(TYPE.TREE);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.write(revTree, output);

        byte[] data = output.toByteArray();
        assertTrue(data.length > 0);

        ObjectReader<RevTree> reader = factory.createRevTreeReader();

        ByteArrayInputStream input = new ByteArrayInputStream(data);
        RevTree serializedTree = reader.read(revTree.getId(), input);

        assertNotNull(serializedTree);
        assertEquals(revTree.size(), serializedTree.size());

        Optional<ImmutableSortedMap<Integer, ObjectId>> optBuckets = revTree.buckets();
        Optional<ImmutableSortedMap<Integer, ObjectId>> optSerializedBuckets = serializedTree
                .buckets();
        assertEquals(optBuckets.isPresent(), optSerializedBuckets.isPresent());
        if (optBuckets.isPresent()) {
            ImmutableSortedMap<Integer, ObjectId> buckets = optBuckets.get();
            ImmutableSortedMap<Integer, ObjectId> serializedBuckets = optSerializedBuckets.get();
            ImmutableSet<Entry<Integer, ObjectId>> entries = buckets.entrySet();
            UnmodifiableIterator<Entry<Integer, ObjectId>> iter = entries.iterator();
            while (iter.hasNext()) {
                Entry<Integer, ObjectId> bucket = iter.next();
                assertTrue(serializedBuckets.containsKey(bucket.getKey()));
                assertEquals(bucket.getValue(), serializedBuckets.get(bucket.getKey()));
            }
        }
        Optional<ImmutableList<Node>> optFeatures = revTree.features();
        Optional<ImmutableList<Node>> optSerializedFeatures = revTree.features();
        assertEquals(optFeatures.isPresent(), optSerializedFeatures.isPresent());
        if (optFeatures.isPresent()) {
            ImmutableList<Node> features = optFeatures.get();
            ImmutableList<Node> serializedfFeatures = optSerializedFeatures.get();
            assertEquals(features.size(), serializedfFeatures.size());
            for (int i = 0; i < features.size(); i++) {
                assertEquals(features.get(i), serializedfFeatures.get(i));
            }
        }
        Optional<ImmutableList<Node>> optTrees = revTree.features();
        Optional<ImmutableList<Node>> optSerializedTrees = revTree.features();
        assertEquals(optFeatures.isPresent(), optSerializedTrees.isPresent());
        if (optTrees.isPresent()) {
            ImmutableList<Node> trees = optTrees.get();
            ImmutableList<Node> serializedTrees = optSerializedTrees.get();
            assertEquals(trees.size(), serializedTrees.size());
            for (int i = 0; i < trees.size(); i++) {
                assertEquals(trees.get(i), serializedTrees.get(i));
            }
        }

    }

    protected abstract ObjectSerialisingFactory getFactory();

}
