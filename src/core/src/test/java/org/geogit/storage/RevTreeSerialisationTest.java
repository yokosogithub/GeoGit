package org.geogit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Envelope;

public abstract class RevTreeSerialisationTest extends Assert {
    private ObjectSerialisingFactory factory = getObjectSerialisingFactory();
    protected abstract ObjectSerialisingFactory getObjectSerialisingFactory();
    
    private RevTree tree1_leaves;
    private RevTree tree2_internal;
    private RevTree tree3_buckets;

    @Before
    public void initialize() {
        ImmutableList<Node> features = ImmutableList.of(Node.create("foo", ObjectId.forString("nodeid"), ObjectId.forString("metadataid"), RevObject.TYPE.FEATURE));
        ImmutableList<Node> trees = ImmutableList.of(Node.create("bar", ObjectId.forString("barnodeid"), ObjectId.forString("barmetadataid"), RevObject.TYPE.TREE));
        ImmutableMap<Integer, Bucket> buckets = ImmutableMap.of(1, Bucket.create(ObjectId.forString("buckettree"), new Envelope()));
        tree1_leaves = RevTreeImpl.createLeafTree(ObjectId.forString("leaves"), 1, features, ImmutableList.<Node>of());
        tree2_internal = RevTreeImpl.createLeafTree(ObjectId.forString("internal"), 1, ImmutableList.<Node>of(), trees);
        tree3_buckets = RevTreeImpl.createNodeTree(ObjectId.forString("buckets"), 1, 1, buckets);
    }
    
    @Test
    public void testRoundTripLeafTree() {
        RevTree roundTripped = read(tree1_leaves.getId(), write(tree1_leaves));
        assertEquals(tree1_leaves, roundTripped);
    }
    
    @Test
    public void testRoundTripInternalTree() {
        RevTree roundTripped = read(tree2_internal.getId(), write(tree2_internal));
        assertEquals(tree2_internal, roundTripped);
    }
    
    @Test
    public void testRoundTripBuckets() {
        RevTree roundTripped = read(tree3_buckets.getId(), write(tree3_buckets));
        assertEquals(tree3_buckets, roundTripped);
    }
    
    private byte[] write(RevTree tree) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectWriter<RevTree> treeWriter = factory.<RevTree> createObjectWriter(RevObject.TYPE.TREE);
            treeWriter.write(tree, bout);
            return bout.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private RevTree read(ObjectId id, byte[] bytes) {
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        ObjectReader<RevTree> treeReader = factory.<RevTree> createObjectReader(RevObject.TYPE.TREE);
        return treeReader.read(id, bin);
    }
}
