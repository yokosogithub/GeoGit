/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.vividsolutions.jts.util.Stopwatch;

public class CommitReaderWriterTest extends RepositoryTestCase {

    RevCommit commit;

    ObjectId treeId;

    ObjectId parentId1;

    ObjectId parentId2;

    @Override
    protected void setUpInternal() throws Exception {
        CommitBuilder b = new CommitBuilder();
        b.setAuthor("groldan");
        b.setAuthorEmail("groldan@opengeo.org");
        b.setCommitter("jdeolive");
        b.setCommitterEmail("jdeolive@opengeo.org");
        b.setMessage("cool this works");
        b.setTimestamp(1000);

        treeId = ObjectId.forString("fake tree content");
        b.setTreeId(treeId);

        parentId1 = ObjectId.forString("fake parent content 1");
        parentId2 = ObjectId.forString("fake parent content 2");
        List<ObjectId> parentIds = Arrays.asList(parentId1, parentId2);
        b.setParentIds(parentIds);

        commit = b.build(ObjectId.NULL);
    }

    @Test
    public void testBackAndForth() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getRepository().newCommitWriter(commit).write(out);
        byte[] built = out.toByteArray();

        RevCommit read = getRepository().newCommitReader().read(ObjectId.NULL,
                new ByteArrayInputStream(built));
        assertNotNull(read);

        assertEquals(commit.getAuthor(), read.getAuthor());
        assertEquals(commit.getCommitter(), read.getCommitter());
        assertEquals(commit.getMessage(), read.getMessage());
        assertEquals(commit.getTimestamp(), read.getTimestamp());
        assertEquals(commit.getTreeId(), read.getTreeId());
        assertEquals(commit.getParentIds(), read.getParentIds());
    }

    @Test
    public void testPerf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int k = 5000;
        Stopwatch sw = new Stopwatch();
        sw.start();
        for (int i = 0; i < k; i++) {
            out.reset();
            getRepository().newCommitWriter(commit).write(out);
        }
        sw.stop();
        // it's at ~1200/s
        System.err.printf("\nBuilt %d commits in %s, (%d/s)\n", k, sw.getTimeString(), k * 1000
                / sw.getTime());

        InputStream built = new ByteArrayInputStream(out.toByteArray());

        sw.start();
        // it's at ~700/s
        for (int i = 0; i < k; i++) {
            built.reset();
            getRepository().newCommitReader().read(ObjectId.NULL, built);
        }
        sw.stop();
        System.err.printf("\nParsed %d commits in %s, (%d/s)\n", k, sw.getTimeString(), k * 1000
                / sw.getTime());
    }
}
