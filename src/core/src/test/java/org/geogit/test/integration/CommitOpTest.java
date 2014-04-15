/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.geogit.api.NodeRef.appendChild;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.geogit.api.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class CommitOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        // These values should be used during a commit to set author/committer
        // TODO: author/committer roles need to be defined better, but for
        // now they are the same thing.
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testInitialCommit() throws Exception {
        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        ObjectId oid1 = insertAndAdd(points1);

        ObjectId oid2 = insertAndAdd(points2);

        geogit.command(AddOp.class).addPattern(".").call();
        RevCommit commit = geogit.command(CommitOp.class).call();
        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(0, commit.getParentIds().size());
        assertFalse(commit.parentN(0).isPresent());
        assertNotNull(commit.getId());
        assertEquals("groldan", commit.getAuthor().getName().get());
        assertEquals("groldan@opengeo.org", commit.getAuthor().getEmail().get());

        ObjectId treeId = commit.getTreeId();

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        Optional<Node> typeTreeId = repo.getTreeChild(root, pointsName);
        assertTrue(typeTreeId.isPresent());

        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        String featureId = points1.getIdentifier().getID();

        String path = NodeRef.appendChild(pointsName, featureId);
        Optional<Node> featureBlobId = repo.getTreeChild(root, path);
        assertTrue(featureBlobId.isPresent());
        assertEquals(oid1, featureBlobId.get().getObjectId());

        featureId = points2.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());
        assertEquals(oid2, featureBlobId.get().getObjectId());

        ObjectId commitId = geogit.command(RevParse.class).setRefSpec(Ref.HEAD).call().get();
        assertEquals(commit.getId(), commitId);
    }

    @Test
    public void testCommitAddsFeatureTypeToObjectDatabase() throws Exception {
        insertAndAdd(points1);
        ObjectId id = RevFeatureType.build(pointsType).getId();
        geogit.command(AddOp.class).addPattern(".").call();
        RevCommit commit = geogit.command(CommitOp.class).call();
        assertNotNull(commit);
        RevFeatureType type = geogit.getRepository().getObjectDatabase().getFeatureType(id);
        assertEquals(id, type.getId());
    }

    @Test
    public void testMultipleCommits() throws Exception {

        // insert and commit points1
        final ObjectId oId1_1 = insertAndAdd(points1);

        geogit.command(AddOp.class).call();
        final RevCommit commit1 = geogit.command(CommitOp.class).call();
        {
            assertCommit(commit1, null, null, null);
            // check points1 is there
            assertEquals(oId1_1, repo.getRootTreeChild(appendChild(pointsName, idP1)).get()
                    .getObjectId());
            // and check the objects were actually copied
            assertNotNull(repo.getObjectDatabase().get(oId1_1));
        }
        // insert and commit points2, points3 and lines1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        geogit.command(AddOp.class).call();
        final RevCommit commit2 = geogit.command(CommitOp.class).setMessage("msg").call();
        {
            assertCommit(commit2, commit1.getId(), "groldan", "msg");

            // repo.getHeadTree().accept(
            // new PrintVisitor(repo.getObjectDatabase(), new PrintWriter(System.out)));

            // check points2, points3 and lines1
            assertEquals(oId1_2, repo.getRootTreeChild(appendChild(pointsName, idP2)).get()
                    .getObjectId());
            assertEquals(oId1_3, repo.getRootTreeChild(appendChild(pointsName, idP3)).get()
                    .getObjectId());
            assertEquals(oId2_1, repo.getRootTreeChild(appendChild(linesName, idL1)).get()
                    .getObjectId());
            // and check the objects were actually copied
            assertNotNull(repo.getObjectDatabase().get(oId1_2));
            assertNotNull(repo.getObjectDatabase().get(oId1_3));
            assertNotNull(repo.getObjectDatabase().get(oId2_1));

            // as well as feature1_1 from the previous commit
            assertEquals(oId1_1, repo.getRootTreeChild(appendChild(pointsName, idP1)).get()
                    .getObjectId());
        }
        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        geogit.command(AddOp.class).call();
        final RevCommit commit3 = geogit.command(CommitOp.class).call();
        {
            assertCommit(commit3, commit2.getId(), "groldan", null);

            // repo.getHeadTree().accept(
            // new PrintVisitor(repo.getObjectDatabase(), new PrintWriter(System.out)));

            // check only points2 and lines2 remain
            assertFalse(repo.getRootTreeChild(appendChild(pointsName, idP1)).isPresent());
            assertFalse(repo.getRootTreeChild(appendChild(pointsName, idP3)).isPresent());
            assertFalse(repo.getRootTreeChild(appendChild(linesName, idL3)).isPresent());

            assertEquals(oId1_2, repo.getRootTreeChild(appendChild(pointsName, idP2)).get()
                    .getObjectId());
            assertEquals(oId2_2, repo.getRootTreeChild(appendChild(linesName, idL2)).get()
                    .getObjectId());
            // and check the objects were actually copied
            assertNotNull(repo.getObjectDatabase().get(oId1_2));
            assertNotNull(repo.getObjectDatabase().get(oId2_2));
        }
    }

    @Test
    public void testCommitWithCustomAuthorAndCommitter() throws Exception {
        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        ObjectId oid1 = insertAndAdd(points1);

        ObjectId oid2 = insertAndAdd(points2);

        geogit.command(AddOp.class).addPattern(".").call();
        CommitOp commitCommand = geogit.command(CommitOp.class);
        commitCommand.setAuthor("John Doe", "John@Doe.com");
        commitCommand.setCommitter("Jane Doe", "Jane@Doe.com");
        RevCommit commit = commitCommand.call();
        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(0, commit.getParentIds().size());
        assertFalse(commit.parentN(0).isPresent());
        assertNotNull(commit.getId());
        assertEquals("John Doe", commit.getAuthor().getName().get());
        assertEquals("John@Doe.com", commit.getAuthor().getEmail().get());
        assertEquals("Jane Doe", commit.getCommitter().getName().get());
        assertEquals("Jane@Doe.com", commit.getCommitter().getEmail().get());

        ObjectId treeId = commit.getTreeId();

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        Optional<Node> typeTreeId = repo.getTreeChild(root, pointsName);
        assertTrue(typeTreeId.isPresent());

        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        String featureId = points1.getIdentifier().getID();
        Optional<Node> featureBlobId = repo.getTreeChild(root,
                NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());
        assertEquals(oid1, featureBlobId.get().getObjectId());

        featureId = points2.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());
        assertEquals(oid2, featureBlobId.get().getObjectId());

        ObjectId commitId = geogit.command(RevParse.class).setRefSpec(Ref.HEAD).call().get();
        assertEquals(commit.getId(), commitId);
    }

    @Test
    public void testCommitWithAllOption() throws Exception {
        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        insertAndAdd(points1);

        geogit.command(AddOp.class).addPattern(".").call();
        RevCommit commit = geogit.command(CommitOp.class).call();

        ObjectId oid = insertAndAdd(points1_modified);

        CommitOp commitCommand = geogit.command(CommitOp.class);
        commit = commitCommand.setAll(true).call();
        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(1, commit.getParentIds().size());
        assertNotNull(commit.getId());

        ObjectId treeId = commit.getTreeId();

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        Optional<Node> typeTreeId = repo.getTreeChild(root, pointsName);
        assertTrue(typeTreeId.isPresent());

        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        String featureId = points1.getIdentifier().getID();
        Optional<Node> featureBlobId = repo.getTreeChild(root,
                NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());
        assertEquals(oid, featureBlobId.get().getObjectId());

        ObjectId commitId = geogit.command(RevParse.class).setRefSpec(Ref.HEAD).call().get();
        assertEquals(commit.getId(), commitId);
    }

    @Test
    public void testCommitWithAllOptionAndPaths() throws Exception {
        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        insertAndAdd(points1);

        geogit.command(AddOp.class).addPattern(".").call();
        RevCommit commit = geogit.command(CommitOp.class).call();

        ObjectId oid = insertAndAdd(points1_modified);
        insert(points2);
        insert(lines1);

        CommitOp commitCommand = geogit.command(CommitOp.class);
        commit = commitCommand.setPathFilters(ImmutableList.of(pointsName)).setAll(true).call();
        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(1, commit.getParentIds().size());
        assertNotNull(commit.getId());

        ObjectId treeId = commit.getTreeId();

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        Optional<Node> linesTreeId = repo.getTreeChild(root, linesName);
        assertFalse(linesTreeId.isPresent());

        Optional<Node> typeTreeId = repo.getTreeChild(root, pointsName);
        assertTrue(typeTreeId.isPresent());

        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        String featureId = points1.getIdentifier().getID();
        Optional<Node> featureBlobId = repo.getTreeChild(root,
                NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());
        assertEquals(oid, featureBlobId.get().getObjectId());

        featureId = points2.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(pointsName, featureId));
        assertFalse(featureBlobId.isPresent());

        ObjectId commitId = geogit.command(RevParse.class).setRefSpec(Ref.HEAD).call().get();
        assertEquals(commit.getId(), commitId);
    }

    @Test
    public void testEmptyCommit() throws Exception {
        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        CommitOp commitCommand = geogit.command(CommitOp.class);
        RevCommit commit = commitCommand.setAllowEmpty(true).call();
        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(0, commit.getParentIds().size());
        assertFalse(commit.parentN(0).isPresent());
        assertNotNull(commit.getId());

        ObjectId commitId = geogit.command(RevParse.class).setRefSpec(Ref.HEAD).call().get();
        assertEquals(commit.getId(), commitId);
    }

    @Test
    public void testNoCommitterName() throws Exception {
        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        repo.getConfigDatabase().remove("user.name");

        CommitOp commitCommand = geogit.command(CommitOp.class);
        exception.expect(IllegalStateException.class);
        commitCommand.setAllowEmpty(true).call();
    }

    @Test
    public void testNoCommitterEmail() throws Exception {
        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        repo.getConfigDatabase().remove("user.email");

        CommitOp commitCommand = geogit.command(CommitOp.class);
        exception.expect(IllegalStateException.class);
        commitCommand.setAllowEmpty(true).call();
    }

    @Test
    public void testCancel() throws Exception {
        ProgressListener listener1 = mock(ProgressListener.class);
        when(listener1.isCanceled()).thenReturn(true);

        ProgressListener listener2 = mock(ProgressListener.class);
        when(listener2.isCanceled()).thenReturn(false, true);

        ProgressListener listener3 = mock(ProgressListener.class);
        when(listener3.isCanceled()).thenReturn(false, false, true);

        try {
            geogit.command(AddOp.class).addPattern(".").call();
            geogit.command(CommitOp.class).call();
            fail("expected NothingToCommitException");
        } catch (NothingToCommitException e) {
            assertTrue(true);
        }

        CommitOp commitCommand1 = geogit.command(CommitOp.class);
        commitCommand1.setProgressListener(listener1);
        assertNull(commitCommand1.setAllowEmpty(true).call());

        CommitOp commitCommand2 = geogit.command(CommitOp.class);
        commitCommand2.setProgressListener(listener2);
        assertNull(commitCommand2.setAllowEmpty(true).call());

        CommitOp commitCommand3 = geogit.command(CommitOp.class);
        commitCommand3.setProgressListener(listener3);
        assertNull(commitCommand3.setAllowEmpty(true).call());
    }

    @Test
    public void testCommitEmptyTreeOnEmptyRepo() throws Exception {
        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        final String emptyTreeName = "emptyTree";

        workingTree.createTypeTree(emptyTreeName, pointsType);
        geogit.command(AddOp.class).addPattern(emptyTreeName).call();

        CommitOp commitCommand = geogit.command(CommitOp.class);
        RevCommit commit = commitCommand.call();
        assertNotNull(commit);

        RevTree head = geogit.command(RevObjectParse.class).setObjectId(commit.getTreeId())
                .call(RevTree.class).get();
        Optional<NodeRef> ref = geogit.command(FindTreeChild.class).setChildPath(emptyTreeName)
                .setParent(head).call();
        assertTrue(ref.isPresent());
    }

    @Test
    public void testCommitEmptyTreeOnNonEmptyRepo() throws Exception {
        insertAndAdd(points1, points2);
        geogit.command(CommitOp.class).call();

        // insertAndAdd(lines1, lines2);

        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        final String emptyTreeName = "emptyTree";

        workingTree.createTypeTree(emptyTreeName, pointsType);
        {
            List<DiffEntry> unstaged = toList(workingTree.getUnstaged(null));
            assertEquals(unstaged.toString(), 1, unstaged.size());
            // assertEquals(NodeRef.ROOT, unstaged.get(0).newName());
            assertEquals(emptyTreeName, unstaged.get(0).newName());
        }
        geogit.command(AddOp.class).call();
        {
            StagingArea index = geogit.getRepository().getIndex();
            List<DiffEntry> staged = toList(index.getStaged(null));
            assertEquals(staged.toString(), 1, staged.size());
            // assertEquals(NodeRef.ROOT, staged.get(0).newName());
            assertEquals(emptyTreeName, staged.get(0).newName());
        }
        CommitOp commitCommand = geogit.command(CommitOp.class);
        RevCommit commit = commitCommand.call();
        assertNotNull(commit);

        RevTree head = geogit.command(RevObjectParse.class).setObjectId(commit.getTreeId())
                .call(RevTree.class).get();
        Optional<NodeRef> ref = geogit.command(FindTreeChild.class).setChildPath(emptyTreeName)
                .setParent(head).call();
        assertTrue(ref.isPresent());
    }

    @Test
    public void testCommitUsingCommit() throws Exception {
        insertAndAdd(points1);
        final RevCommit commit = geogit.command(CommitOp.class)
                .setCommitter("anothercommitter", "anothercommitter@opengeo.org").call();
        insertAndAdd(points2);
        RevCommit commit2 = geogit.command(CommitOp.class).setCommit(commit).call();
        assertEquals(commit.getMessage(), commit2.getMessage());
        assertEquals(commit.getAuthor(), commit2.getAuthor());
        assertNotSame(commit.getCommitter(), commit2.getCommitter());
    }

    @Test
    public void testCommitUsingCommitAndMessage() throws Exception {
        String message = "A message";
        insertAndAdd(points1);
        final RevCommit commit = geogit.command(CommitOp.class)
                .setCommitter("anothercommitter", "anothercommitter@opengeo.org").call();
        insertAndAdd(points2);
        RevCommit commit2 = geogit.command(CommitOp.class).setCommit(commit).setMessage(message)
                .call();
        assertNotSame(commit.getMessage(), commit2.getMessage());
        assertEquals(commit.getAuthor(), commit2.getAuthor());
        assertNotSame(commit.getCommitter(), commit2.getCommitter());
        assertEquals(message, commit2.getMessage());
    }

    @Test
    public void testCommitWithDeletedTree() throws Exception {
        insertAndAdd(points1, points2);
        insertAndAdd(lines1, lines2);
        final RevCommit commit1 = geogit.command(CommitOp.class).call();

        final RevTree tree1 = geogit.command(RevObjectParse.class).setObjectId(commit1.getTreeId())
                .call(RevTree.class).get();
        assertTrue(tree1.trees().isPresent());
        assertEquals(2, tree1.trees().get().size());

        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        workingTree.delete(pointsName);
        geogit.command(AddOp.class).call();

        final RevCommit commit2 = geogit.command(CommitOp.class).call();

        RevTree tree2 = geogit.command(RevObjectParse.class).setObjectId(commit2.getTreeId())
                .call(RevTree.class).get();

        assertTrue(tree2.trees().isPresent());
        assertEquals(1, tree2.trees().get().size());
    }

    @Test
    public void testAmend() throws Exception {

        final ObjectId id = insertAndAdd(points1);
        final RevCommit commit1 = geogit.command(CommitOp.class).setMessage("Message").call();
        {
            assertCommit(commit1, null, null, null);
            assertEquals(id, repo.getRootTreeChild(appendChild(pointsName, idP1)).get()
                    .getObjectId());
            assertNotNull(repo.getObjectDatabase().get(id));
        }

        final ObjectId id2 = insertAndAdd(points2);
        final RevCommit commit2 = geogit.command(CommitOp.class).setAmend(true).call();
        {
            assertCommit(commit2, null, "groldan", "Message");
            Optional<RevFeature> p2 = geogit.command(RevObjectParse.class)
                    .setRefSpec("HEAD:" + appendChild(pointsName, idP2)).call(RevFeature.class);
            assertTrue(p2.isPresent());
            assertEquals(id2, p2.get().getId());
            Optional<RevFeature> p1 = geogit.command(RevObjectParse.class)
                    .setRefSpec("HEAD:" + appendChild(pointsName, idP1)).call(RevFeature.class);
            assertTrue(p1.isPresent());
            assertEquals(id, p1.get().getId());
        }
        Iterator<RevCommit> log = geogit.command(LogOp.class).call();
        assertTrue(log.hasNext());
        log.next();
        assertFalse(log.hasNext());

    }

    @Test
    public void testCannotAmend() throws Exception {

        insertAndAdd(points1);
        try {
            geogit.command(CommitOp.class).setAmend(true).call();
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(true);

        }

    }

    private void assertCommit(RevCommit commit, @Nullable ObjectId parentId, String author,
            String message) {

        assertNotNull(commit);
        assertEquals(parentId == null ? 0 : 1, commit.getParentIds().size());
        assertEquals(parentId, commit.parentN(0).orNull());
        assertNotNull(commit.getTreeId());
        assertNotNull(commit.getId());
        if (author != null) {
            assertEquals(author, commit.getAuthor().getName().get());
        }
        if (message != null) {
            assertEquals(message, commit.getMessage());
        }
        assertNotNull(repo.getTree(commit.getTreeId()));
        assertEquals(commit.getId(), getRepository().getRef(Ref.HEAD).get().getObjectId());
    }

    @Test
    public void testPathFiltering() throws Exception {
        insertAndAdd(points1);
        insertAndAdd(points2);

        RevCommit commit = geogit.command(CommitOp.class).call();

        insertAndAdd(points3);

        insertAndAdd(lines1);
        insertAndAdd(lines2);
        insertAndAdd(lines3);

        List<String> filters = Arrays.asList("Points/Points.3", "Lines/Lines.1", "Lines/Lines.3");
        commit = geogit.command(CommitOp.class).setPathFilters(filters).call();

        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(1, commit.getParentIds().size());
        assertNotNull(commit.getId());

        ObjectId treeId = commit.getTreeId();

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        Optional<Node> typeTreeId = repo.getTreeChild(root, pointsName);
        assertTrue(typeTreeId.isPresent());
        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        String featureId = points1.getIdentifier().getID();
        Optional<Node> featureBlobId = repo.getTreeChild(root,
                NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());

        featureId = points2.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());

        featureId = points3.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());

        typeTreeId = repo.getTreeChild(root, linesName);
        assertTrue(typeTreeId.isPresent());
        typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        featureId = lines1.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(linesName, featureId));
        assertTrue(featureBlobId.isPresent());

        featureId = lines2.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(linesName, featureId));
        assertFalse(featureBlobId.isPresent());

        featureId = lines3.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(linesName, featureId));
        assertTrue(featureBlobId.isPresent());
    }

    @Test
    public void testPathFilteringWithUnstaged() throws Exception {
        insertAndAdd(points1);
        insertAndAdd(points2);

        RevCommit commit = geogit.command(CommitOp.class).call();

        insertAndAdd(lines1);
        insertAndAdd(lines3);
        insert(lines2);
        insert(points3);

        List<String> filters = Arrays.asList(pointsName, linesName);
        commit = geogit.command(CommitOp.class).setPathFilters(filters).call();

        assertNotNull(commit);
        assertNotNull(commit.getParentIds());
        assertEquals(1, commit.getParentIds().size());
        assertNotNull(commit.getId());

        ObjectId treeId = commit.getTreeId();

        assertNotNull(treeId);
        RevTree root = repo.getTree(treeId);
        assertNotNull(root);

        Optional<Node> typeTreeId = repo.getTreeChild(root, pointsName);
        assertTrue(typeTreeId.isPresent());
        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        String featureId = points1.getIdentifier().getID();
        Optional<Node> featureBlobId = repo.getTreeChild(root,
                NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());

        featureId = points2.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(pointsName, featureId));
        assertTrue(featureBlobId.isPresent());

        featureId = points3.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(pointsName, featureId));
        assertFalse(featureBlobId.isPresent());

        typeTreeId = repo.getTreeChild(root, linesName);
        assertTrue(typeTreeId.isPresent());
        typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);

        featureId = lines1.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(linesName, featureId));
        assertTrue(featureBlobId.isPresent());

        featureId = lines2.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(linesName, featureId));
        assertFalse(featureBlobId.isPresent());

        featureId = lines3.getIdentifier().getID();
        featureBlobId = repo.getTreeChild(root, NodeRef.appendChild(linesName, featureId));
        assertTrue(featureBlobId.isPresent());
    }

}
