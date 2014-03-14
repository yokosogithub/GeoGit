/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api;

import static org.geogit.api.NodeRef.allPathsTo;
import static org.geogit.api.NodeRef.isChild;
import static org.geogit.api.NodeRef.isDirectChild;
import static org.geogit.api.NodeRef.parentPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geogit.api.RevObject.TYPE;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class NodeRefTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Test method for {@link org.geogit.api.Node#parentPath(java.lang.String)}.
     */
    @Test
    public void testParentPath() {
        assertNull(parentPath(null));
        assertNull(parentPath(""));
        assertEquals("", parentPath("node"));
        assertEquals("to", parentPath("to/node"));
        assertEquals("path/to", parentPath("path/to/node"));
    }

    /**
     * Test method for {@link org.geogit.api.Node#allPathsTo(java.lang.String)}.
     */
    @Test
    public void testAllPathsTo() {
        try {
            allPathsTo(null);
            fail("Expected precondition violation");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        try {
            allPathsTo("");
            fail("Expected precondition violation");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        assertEquals(ImmutableList.of("path"), allPathsTo("path"));
        assertEquals(ImmutableList.of("path", "path/to"), allPathsTo("path/to"));
        assertEquals(ImmutableList.of("path", "path/to", "path/to/node"),
                allPathsTo("path/to/node"));
    }

    /**
     * Test method for {@link org.geogit.api.Node#isDirectChild(String, String)}
     */
    @Test
    public void testIsDirectChild() {
        assertFalse(isDirectChild("", ""));
        assertTrue(isDirectChild("", "path"));
        assertFalse(isDirectChild("", "path/to"));

        assertFalse(isDirectChild("path", "path"));
        assertFalse(isDirectChild("path", ""));
        assertTrue(isDirectChild("path", "path/to"));
        assertFalse(isDirectChild("path", "path/to/node"));

        assertFalse(isDirectChild("path/to", ""));
        assertFalse(isDirectChild("path/to", "path"));
        assertFalse(isDirectChild("path/to", "path/to"));
        assertFalse(isDirectChild("path/to", "path2/to"));

        assertTrue(isDirectChild("path/to", "path/to/node"));

        assertTrue(isDirectChild("roads", "roads/highway"));
        assertFalse(isDirectChild("roads/highway", "roads"));
    }

    /**
     * Test method for {@link org.geogit.api.Node#isChild(String, String)}
     */
    @Test
    public void testIsChild() {
        assertFalse(isChild("", ""));
        assertTrue(isChild("", "path"));
        assertTrue(isChild("", "path/to"));

        assertFalse(isChild("path", "path"));
        assertFalse(isChild("path", ""));
        assertTrue(isChild("path", "path/to"));
        assertTrue(isChild("path", "path/to/node"));

        assertFalse(isChild("path/to", ""));
        assertFalse(isChild("path/to", "path"));
        assertFalse(isChild("path/to", "path/to"));
        assertFalse(isChild("path/to", "path2/to"));

        assertTrue(isChild("path/to", "path/to/node"));
    }

    @Test
    public void testCheckValidPathNull() {
        exception.expect(IllegalArgumentException.class);
        NodeRef.checkValidPath(null);
    }

    @Test
    public void testCheckValidPathEmptyString() {
        exception.expect(IllegalArgumentException.class);
        NodeRef.checkValidPath("");
    }

    @Test
    public void testCheckValidPathPathEndingWithSeperator() {
        exception.expect(IllegalArgumentException.class);
        NodeRef.checkValidPath("Points/");
    }

    @Test
    public void testCheckValidPath() {
        NodeRef.checkValidPath("Points");
    }

    @Test
    public void testNodeFromPath() {
        String node = NodeRef.nodeFromPath("Points/Points.1");
        assertEquals(node, "Points.1");
        node = NodeRef.nodeFromPath("refs/heads/master");
        assertEquals(node, "master");
        node = NodeRef.nodeFromPath("Points.1");
        assertEquals(node, "Points.1");
        node = NodeRef.nodeFromPath("");
        assertNull(node);
        node = NodeRef.nodeFromPath(null);
        assertNull(node);
    }

    @Test
    public void testAppendChild() {
        String fullString = NodeRef.appendChild("Points", "Points.1");
        assertEquals(fullString, "Points/Points.1");
        fullString = NodeRef.appendChild("", "refs");
        assertEquals(fullString, "refs");
    }

    @Test
    public void testAccessorsAndConstructors() {
        Node node = Node.create("Points.1", ObjectId.forString("Points stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        NodeRef nodeRef = new NodeRef(node, "Points", ObjectId.NULL);
        assertEquals(node.getMetadataId(), Optional.absent());
        assertEquals(node.getName(), nodeRef.name());
        assertEquals(node.getObjectId(), nodeRef.objectId());
        assertEquals(node, nodeRef.getNode());
        assertEquals(node.getType(), nodeRef.getType());
        assertEquals(nodeRef.getParentPath(), "Points");
        assertEquals(nodeRef.path(), "Points/Points.1");
    }

    @Test
    public void testIsEqual() {
        Node node = Node.create("Points.1", ObjectId.forString("Points stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        NodeRef nodeRef = new NodeRef(node, "Points", ObjectId.NULL);
        assertFalse(nodeRef.equals(node));
        Node node2 = Node.create("Lines.1", ObjectId.forString("Lines stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        NodeRef nodeRef2 = new NodeRef(node2, "Lines", ObjectId.NULL);
        NodeRef nodeRef3 = new NodeRef(node2, "Lines", ObjectId.forString("Lines stuff"));
        assertFalse(nodeRef.equals(nodeRef2));
        assertTrue(nodeRef.equals(nodeRef));
        assertFalse(nodeRef2.equals(nodeRef3));
    }

    @Test
    public void testNodeAndNodeRefToString() {
        Node node = Node.create("Points.1", ObjectId.forString("Points stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        NodeRef nodeRef = new NodeRef(node, "Points", ObjectId.NULL);

        String readableNode = nodeRef.toString();

        assertTrue(readableNode.equals("NodeRef[Points/Points.1 -> "
                + node.getObjectId().toString() + "]"));
    }

    @Test
    public void testCompareTo() {
        Node node = Node.create("Points.1", ObjectId.forString("Points stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        NodeRef nodeRef = new NodeRef(node, "Points", ObjectId.NULL);
        assertFalse(nodeRef.equals(node));
        Node node2 = Node.create("Lines.1", ObjectId.forString("Lines stuff"), ObjectId.NULL,
                TYPE.FEATURE, null);
        NodeRef nodeRef2 = new NodeRef(node2, "Lines", ObjectId.NULL);
        assertTrue(nodeRef.compareTo(nodeRef2) > 0);
        assertTrue(nodeRef2.compareTo(nodeRef) < 0);
        assertTrue(nodeRef.compareTo(nodeRef) == 0);
    }

}
