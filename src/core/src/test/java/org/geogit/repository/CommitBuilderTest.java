/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.List;

import junit.framework.TestCase;

import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;

import com.google.common.collect.ImmutableList;

public class CommitBuilderTest extends TestCase {

    @Override
    protected void setUp() throws Exception {

    }

    public void testBuildEmpty() throws Exception {
        CommitBuilder b = new CommitBuilder();
        try {
            b.build();
            fail("expected IllegalStateException on null tree id");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("tree"));
        }
    }

    public void testBuildFull() throws Exception {
        CommitBuilder b = new CommitBuilder();
        b.setAuthor("groldan");
        b.setAuthorEmail("groldan@opengeo.org");
        b.setCommitter("jdeolive");
        b.setCommitterEmail("jdeolive@opengeo.org");
        b.setMessage("cool this works");
        b.setCommitterTimestamp(1000);
        b.setCommitterTimeZoneOffset(10);
        b.setAuthorTimestamp(500);
        b.setAuthorTimeZoneOffset(-5);

        ObjectId treeId = ObjectId.forString("fake tree content");

        b.setTreeId(treeId);

        ObjectId parentId1 = ObjectId.forString("fake parent content 1");
        ObjectId parentId2 = ObjectId.forString("fake parent content 2");
        List<ObjectId> parentIds = ImmutableList.of(parentId1, parentId2);
        b.setParentIds(parentIds);

        RevCommit build = b.build();
        assertNotNull(build.getId());
        assertFalse(build.getId().isNull());
        assertEquals(treeId, build.getTreeId());
        assertEquals(parentIds, build.getParentIds());
        assertEquals("groldan", build.getAuthor().getName().get());
        assertEquals("groldan@opengeo.org", build.getAuthor().getEmail().get());
        assertEquals("jdeolive", build.getCommitter().getName().get());
        assertEquals("jdeolive@opengeo.org", build.getCommitter().getEmail().get());
        assertEquals("cool this works", build.getMessage());
        assertEquals(1000L, build.getCommitter().getTimestamp());
        assertEquals(10, build.getCommitter().getTimeZoneOffset());
        assertEquals(500L, build.getAuthor().getTimestamp());
        assertEquals(-5, build.getAuthor().getTimeZoneOffset());
    }
}
