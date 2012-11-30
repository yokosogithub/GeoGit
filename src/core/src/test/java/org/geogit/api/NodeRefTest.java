/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
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

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class NodeRefTest {

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
}
