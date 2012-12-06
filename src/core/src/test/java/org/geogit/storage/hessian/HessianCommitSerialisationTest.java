/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;

public class HessianCommitSerialisationTest extends TestCase {
    public void testCommitRoundTrippin() throws Exception {
        long currentTime = System.currentTimeMillis();
        CommitBuilder builder = new CommitBuilder();
        String author = "groldan";
        builder.setAuthor(author);
        String authorEmail = "groldan@opengeo.org";
        builder.setAuthorEmail(authorEmail);
        String committer = "mleslie";
        builder.setCommitter(committer);
        String committerEmail = "mleslie@opengeo.org";
        builder.setCommitterEmail(committerEmail);
        builder.setTimestamp(currentTime);

        ObjectId treeId = ObjectId.forString("Fake tree");
        builder.setTreeId(treeId);

        ObjectId parent1 = ObjectId.forString("Parent 1 of fake commit");
        ObjectId parent2 = ObjectId.forString("Parent 2 of fake commit");
        List<ObjectId> parents = Arrays.asList(parent1, parent2);
        builder.setParentIds(parents);

        RevCommit cmtIn = builder.build();
        assertNotNull(cmtIn);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        HessianCommitWriter write = new HessianCommitWriter();
        write.write(cmtIn, bout);

        byte[] bytes = bout.toByteArray();
        assertTrue(bytes.length > 0);

        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        HessianCommitReader read = new HessianCommitReader();

        RevCommit cmtOut = read.read(cmtIn.getId(), bin);

        assertEquals(treeId, cmtOut.getTreeId());
        assertEquals(parents, cmtOut.getParentIds());
        assertEquals(author, cmtOut.getAuthor().getName());
        assertEquals(authorEmail, cmtOut.getAuthor().getEmail());
        assertEquals(committer, cmtOut.getCommitter().getName());
        assertEquals(committerEmail, cmtOut.getCommitter().getEmail());
        assertEquals(currentTime, cmtOut.getTimestamp());

    }
}
