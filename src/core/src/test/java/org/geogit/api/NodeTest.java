/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geogit.api.RevObject.TYPE;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class NodeTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testNodeAccessorsAndConstructors() {
        ObjectId oid = ObjectId.forString("Points stuff");
        Node node = Node.create("Points", oid, ObjectId.NULL, TYPE.TREE, null);
        assertEquals(node.getMetadataId(), Optional.absent());
        assertEquals(node.getName(), "Points");
        assertEquals(node.getObjectId(), oid);
        assertEquals(node.getType(), TYPE.TREE);
    }

    @Test
    public void testIsEqual() {
        Node node = Node.create("Points.1", ObjectId.forString("Points stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        Node node2 = Node.create("Lines.1", ObjectId.forString("Lines stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        Node node3 = Node.create("Lines.1", ObjectId.forString("Lines stuff"),
                ObjectId.forString("Lines Stuff"), TYPE.TREE, null);

        assertFalse(node.equals("NotANode"));
        assertFalse(node.equals(node2));
        assertFalse(node2.equals(node3));
        assertTrue(node.equals(node));
    }

    @Test
    public void testToString() {
        Node node = Node.create("Points.1", ObjectId.forString("Points stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);

        String readableNode = node.toString();
        String expected = "FeatureNode[Points.1 -> " + node.getObjectId() + "]";
        assertEquals(expected, readableNode.toString());
    }

    @Test
    public void testCompareTo() {
        Node node = Node.create("Points.1", ObjectId.forString("Points stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        Node node2 = Node.create("Lines.1", ObjectId.forString("Lines stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);

        assertTrue(node.compareTo(node2) > 0);
        assertTrue(node2.compareTo(node) < 0);
        assertTrue(node.compareTo(node) == 0);
    }
}
