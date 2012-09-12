/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.repository.StagingArea;
import org.junit.Test;

public class CommitOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testInitialCommit() throws Exception {
        try {
            geogit.add().addPattern(".").call();
            geogit.commit().setAuthor("groldan").call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        StagingArea index = repo.getIndex();

        ObjectId oid1 = insertAndAdd(points1);
        // BLOBS.print(repo.getRawObject(insertedId1), System.err);

        ObjectId oid2 = insertAndAdd(points2);
        // BLOBS.print(repo.getRawObject(insertedId2), System.err);

        geogit.add().addPattern(".").call();
        RevCommit commit = geogit.commit().setAuthor("groldan").call();
        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(1, commit.getParentIds().size());
        assertTrue(commit.getParentIds().get(0).isNull());
        assertNotNull(commit.getId());
        assertEquals("groldan", commit.getAuthor());

        ObjectId treeId = commit.getTreeId();
        // BLOBS.print(repo.getRawObject(treeId), System.err);

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        NodeRef nsTreeId = root.get(pointsNs);
        assertNotNull(nsTreeId);
        // BLOBS.print(repo.getRawObject(nsTreeId), System.err);
        RevTree nstree = repo.getTree(nsTreeId.getObjectId());
        assertNotNull(nstree);

        NodeRef typeTreeId = nstree.get(pointsName);
        assertNotNull(typeTreeId);
        // BLOBS.print(repo.getRawObject(typeTreeId), System.err);
        RevTree typeTree = repo.getTree(typeTreeId.getObjectId());
        assertNotNull(typeTree);

        String featureId = points1.getIdentifier().getID();
        NodeRef featureBlobId = typeTree.get(featureId);
        assertNotNull(featureBlobId);
        assertEquals(oid1, featureBlobId.getObjectId());

        ObjectId commitId = geogit.command(RevParse.class).setRefSpec(Ref.HEAD).call();
        assertEquals(commit.getId(), commitId);
    }

    @Test
    public void testMultipleCommits() throws Exception {

        // insert and commit points1
        final ObjectId oId1_1 = insertAndAdd(points1);

        geogit.add().call();
        final RevCommit commit1 = geogit.commit().setAuthor("groldan").call();
        {
            assertCommit(commit1, ObjectId.NULL, "groldan", null);
            // check points1 is there
            assertEquals(oId1_1, repo.getRootTreeChild(pointsNs, pointsName, idP1).getObjectId());
            // and check the objects were actually copied
            assertNotNull(repo.getObjectDatabase().getRaw(oId1_1));
        }
        // insert and commit points2, points3 and lines1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        geogit.add().call();
        final RevCommit commit2 = geogit.commit().setAuthor("groldan").setMessage("msg").call();
        {
            assertCommit(commit2, commit1.getId(), "groldan", "msg");

            // repo.getHeadTree().accept(
            // new PrintVisitor(repo.getObjectDatabase(), new PrintWriter(System.out)));

            // check points2, points3 and lines1
            assertEquals(oId1_2, repo.getRootTreeChild(pointsNs, pointsName, idP2).getObjectId());
            assertEquals(oId1_3, repo.getRootTreeChild(pointsNs, pointsName, idP3).getObjectId());
            assertEquals(oId2_1, repo.getRootTreeChild(linesNs, linesName, idL1).getObjectId());
            // and check the objects were actually copied
            assertNotNull(repo.getObjectDatabase().getRaw(oId1_2));
            assertNotNull(repo.getObjectDatabase().getRaw(oId1_3));
            assertNotNull(repo.getObjectDatabase().getRaw(oId2_1));

            // as well as feature1_1 from the previous commit
            assertEquals(oId1_1, repo.getRootTreeChild(pointsNs, pointsName, idP1).getObjectId());
        }
        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        geogit.add().call();
        final RevCommit commit3 = geogit.commit().setAuthor("groldan").call();
        {
            assertCommit(commit3, commit2.getId(), "groldan", null);

            // repo.getHeadTree().accept(
            // new PrintVisitor(repo.getObjectDatabase(), new PrintWriter(System.out)));

            // check only points2 and lines2 remain
            assertNull(repo.getRootTreeChild(pointsNs, pointsName, idP1));
            assertNull(repo.getRootTreeChild(pointsNs, pointsName, idP3));
            assertNull(repo.getRootTreeChild(linesNs, linesName, idL3));

            assertEquals(oId1_2, repo.getRootTreeChild(pointsNs, pointsName, idP2).getObjectId());
            assertEquals(oId2_2, repo.getRootTreeChild(linesNs, linesName, idL2).getObjectId());
            // and check the objects were actually copied
            assertNotNull(repo.getObjectDatabase().getRaw(oId1_2));
            assertNotNull(repo.getObjectDatabase().getRaw(oId2_2));
        }
    }

    private void assertCommit(RevCommit commit, ObjectId parentId, String author, String message) {
        assertNotNull(commit);
        assertEquals(1, commit.getParentIds().size());
        assertEquals(parentId, commit.getParentIds().get(0));
        assertNotNull(commit.getTreeId());
        assertNotNull(commit.getId());
        if (author != null) {
            assertEquals(author, commit.getAuthor());
        }
        if (message != null) {
            assertEquals(message, commit.getMessage());
        }
        assertNotNull(repo.getTree(commit.getTreeId()));
        assertEquals(commit.getId(), getRepository().getRef(Ref.HEAD).getObjectId());
    }

}
