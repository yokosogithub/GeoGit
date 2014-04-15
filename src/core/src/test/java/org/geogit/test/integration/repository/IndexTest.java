/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration.repository;

import static org.geogit.api.NodeRef.appendChild;

import java.util.Collection;
import java.util.List;

import org.geogit.api.CommitBuilder;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.WriteTree;
import org.geogit.api.porcelain.AddOp;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.ObjectInserter;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

public class IndexTest extends RepositoryTestCase {

    private StagingArea index;

    @Override
    protected void setUpInternal() throws Exception {
        index = repo.getIndex();
    }

    // two features with the same content and different fid should point to the same object
    @Test
    public void testInsertIdenticalObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        Feature equalContentFeature = feature(pointsType, "DifferentId", ((SimpleFeature) points1)
                .getAttributes().toArray());

        ObjectId oId2 = insertAndAdd(equalContentFeature);

        // BLOBS.print(repo.getRawObject(insertedId1), System.err);
        // BLOBS.print(repo.getRawObject(insertedId2), System.err);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertEquals(oId1, oId2);
    }

    // two features with different content should point to different objects
    @Test
    public void testInsertNonEqualObjects() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);

        ObjectId oId2 = insertAndAdd(points2);
        assertNotNull(oId1);
        assertNotNull(oId2);
        assertFalse(oId1.equals(oId2));
    }

    @Test
    public void testWriteTree() throws Exception {

        insertAndAdd(points1);
        insertAndAdd(lines1);

        // this new root tree must exist on the repo db, but is not set as the current head. In
        // fact, it is headless, as there's no commit pointing to it. CommitOp does that.
        ObjectId newRootTreeId = geogit.command(WriteTree.class)
                .setOldRoot(tree(repo.getHead().get().getObjectId())).call();

        assertNotNull(newRootTreeId);
        assertFalse(repo.getRootTreeId().equals(newRootTreeId));
        // but the index staged root shall be pointing to it
        // assertEquals(newRootTreeId, index.getStaged().getId());

        RevTree tree = repo.getTree(newRootTreeId);
        // assertEquals(2, tree.size().intValue());

        String path = appendChild(pointsName, points1.getIdentifier().getID());
        assertTrue(repo.command(FindTreeChild.class).setParent(tree).setChildPath(path).call()
                .isPresent());

        path = appendChild(linesName, lines1.getIdentifier().getID());
        assertTrue(repo.command(FindTreeChild.class).setParent(tree).setChildPath(path).call()
                .isPresent());

        // simulate a commit so the repo head points to this new tree
        ObjectInserter objectInserter = repo.newObjectInserter();
        List<ObjectId> parents = ImmutableList.of();

        RevCommit commit = new CommitBuilder(geogit.getPlatform()).setTreeId(newRootTreeId)
                .setParentIds(parents).build();
        ObjectId commitId = commit.getId();

        objectInserter.insert(commit);
        Optional<Ref> newHead = geogit.command(UpdateRef.class).setName("refs/heads/master")
                .setNewValue(commitId).call();
        assertTrue(newHead.isPresent());

        WorkingTree workTree = repo.getWorkingTree();
        workTree.delete(linesName, lines1.getIdentifier().getID());
        geogit.command(AddOp.class).call();

        newRootTreeId = geogit.command(WriteTree.class).setOldRoot(tree(newRootTreeId)).call(); // newRootTreeId
                                                                                                // =
                                                                                                // index.writeTree(newRootTreeId,
                                                                                                // new
                                                                                                // NullProgressListener());

        assertNotNull(newRootTreeId);
        assertFalse(repo.getRootTreeId().equals(newRootTreeId));

        tree = repo.getTree(newRootTreeId);

        path = appendChild(pointsName, points1.getIdentifier().getID());
        assertTrue(repo.command(FindTreeChild.class).setParent(tree).setChildPath(path).call()
                .isPresent());

        path = appendChild(linesName, lines1.getIdentifier().getID());
        assertFalse(repo.command(FindTreeChild.class).setParent(tree).setChildPath(path).call()
                .isPresent());

    }

    private static class TreeNameFilter implements Predicate<NodeRef> {

        private String treePath;

        public TreeNameFilter(String treePath) {
            this.treePath = treePath;
        }

        @Override
        public boolean apply(NodeRef ref) {
            TYPE type = ref.getType();
            String path = ref.path();
            return TYPE.TREE.equals(type) && treePath.equals(path);
        }
    }

    @Test
    public void testWriteEmptyPathAddAll() throws Exception {
        insert(lines1);
        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        workingTree.createTypeTree(pointsName, pointsType);

        List<NodeRef> workHead = toList(geogit.command(LsTreeOp.class).setReference(Ref.WORK_HEAD)
                .setStrategy(Strategy.DEPTHFIRST).call());

        assertEquals(3, workHead.size());
        Collection<NodeRef> filtered = Collections2
                .filter(workHead, new TreeNameFilter(pointsName));
        assertEquals(1, filtered.size());

        geogit.command(AddOp.class).call();

        List<NodeRef> indexHead = toList(geogit.command(LsTreeOp.class)
                .setReference(Ref.STAGE_HEAD).setStrategy(Strategy.DEPTHFIRST).call());

        assertEquals(3, indexHead.size());
        filtered = Collections2.filter(indexHead, new TreeNameFilter(pointsName));
        assertEquals(1, filtered.size());
    }

    @Test
    public void testWriteEmptyPath() throws Exception {
        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        workingTree.createTypeTree(pointsName, pointsType);
        workingTree.createTypeTree(linesName, linesType);

        List<NodeRef> workHead = toList(geogit.command(LsTreeOp.class).setReference(Ref.WORK_HEAD)
                .setStrategy(Strategy.DEPTHFIRST).call());

        assertEquals(2, workHead.size());

        Collection<NodeRef> filtered;
        filtered = Collections2.filter(workHead, new TreeNameFilter(pointsName));
        assertEquals(1, filtered.size());

        filtered = Collections2.filter(workHead, new TreeNameFilter(linesName));
        assertEquals(1, filtered.size());

        geogit.command(AddOp.class).addPattern(pointsName).call();

        List<NodeRef> indexHead;
        indexHead = toList(geogit.command(LsTreeOp.class).setReference(Ref.STAGE_HEAD)
                .setStrategy(Strategy.DEPTHFIRST).call());

        assertEquals(1, indexHead.size());
        filtered = Collections2.filter(indexHead, new TreeNameFilter(pointsName));
        assertEquals(1, filtered.size());

        geogit.command(AddOp.class).addPattern(linesName).call();
        indexHead = toList(geogit.command(LsTreeOp.class).setReference(Ref.STAGE_HEAD)
                .setStrategy(Strategy.DEPTHFIRST).call());

        assertEquals(2, indexHead.size());// Points and Lines
        filtered = Collections2.filter(indexHead, new TreeNameFilter(linesName));
        assertEquals(1, filtered.size());
    }

    @Test
    public void testModify() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        assertNotNull(oId1);

        assertEquals(oId1, index.findStaged(appendChild(pointsName, idP1)).get().getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        assertNotNull(oId1_modified);
        assertFalse(oId1.equals(oId1_modified));

        assertFalse(index.findStaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oId1));
        assertEquals(oId1_modified, index.findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

    }

    @Test
    public void testAddMultiple() throws Exception {
        ObjectId oId1 = insert(points1);
        ObjectId oId2 = insert(points2);
        assertNotNull(oId1);
        assertNotNull(oId2);

        assertFalse(index.findStaged(appendChild(pointsName, idP1)).isPresent());
        assertFalse(index.findStaged(appendChild(pointsName, idP2)).isPresent());

        assertEquals(oId1, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());

        geogit.command(AddOp.class).call();

        assertEquals(oId1, index.findStaged(appendChild(pointsName, idP1)).get().getObjectId());
        assertEquals(oId2, index.findStaged(appendChild(pointsName, idP2)).get().getObjectId());

    }

    private Supplier<RevTree> tree(final ObjectId treeId) {
        Supplier<RevTree> delegate = new Supplier<RevTree>() {

            @Override
            public RevTree get() {
                if (treeId.isNull()) {
                    return RevTree.EMPTY;
                }
                return geogit.command(RevObjectParse.class).setObjectId(treeId).call(RevTree.class)
                        .get();
            }
        };
        return Suppliers.memoize(delegate);
    }

    @Test
    public void testMultipleStaging() throws Exception {

        // insert and commit feature1_1
        final ObjectId oId1_1 = insertAndAdd(points1);

        System.err.println("++++++++++++ stage 1:  ++++++++++++++++++++");
        // staged1.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // check feature1_1 is there
        assertEquals(oId1_1, index.findStaged(appendChild(pointsName, idP1)).get().getObjectId());

        // insert and commit feature1_2, feature1_2 and feature2_1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        System.err.println("++++++++++++ stage 2: ++++++++++++++++++++");
        // staged2.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // check feature1_2, feature1_3 and feature2_1
        Optional<Node> treeChild;

        assertNotNull(treeChild = index.findStaged(appendChild(pointsName, idP2)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId1_2, treeChild.get().getObjectId());

        assertNotNull(treeChild = index.findStaged(appendChild(pointsName, idP3)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId1_3, treeChild.get().getObjectId());

        assertNotNull(treeChild = index.findStaged(appendChild(linesName, idL1)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId2_1, treeChild.get().getObjectId());

        // as well as feature1_1 from the previous commit
        assertNotNull(treeChild = index.findStaged(appendChild(pointsName, idP1)));
        assertTrue(treeChild.isPresent());
        assertEquals(oId1_1, treeChild.get().getObjectId());

        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        System.err.println("++++++++++++ stage 3: ++++++++++++++++++++");
        // staged3.accept(new PrintVisitor(index.getDatabase(), new PrintWriter(System.err)));

        // and check only points2 and lines2 remain (i.e. its oids are set to NULL)
        assertFalse(index.findStaged(appendChild(pointsName, idP1)).isPresent());
        assertFalse(index.findStaged(appendChild(pointsName, idP3)).isPresent());
        assertFalse(index.findStaged(appendChild(linesName, idL1)).isPresent());

        assertEquals(oId1_2, index.findStaged(appendChild(pointsName, idP2)).get().getObjectId());
        assertEquals(oId2_2, index.findStaged(appendChild(linesName, idL2)).get().getObjectId());

    }

    @Test
    public void testWriteTree2() throws Exception {

        // insert and commit feature1_1
        final ObjectId oId1_1 = insertAndAdd(points1);

        final ObjectId newRepoTreeId1;
        {
            newRepoTreeId1 = geogit.command(WriteTree.class)
                    .setOldRoot(tree(repo.getHead().get().getObjectId())).call();

            // assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId1);

            RevTree newRepoTree = repo.getTree(newRepoTreeId1);

            System.err.println("++++++++++ new repo tree 1: " + newRepoTreeId1 + " ++++++++++++");
            // check feature1_1 is there
            assertEquals(oId1_1, repo.getTreeChild(newRepoTree, appendChild(pointsName, idP1))
                    .get().getObjectId());

        }

        // insert and add (stage) points2, points3, and lines1
        final ObjectId oId1_2 = insertAndAdd(points2);
        final ObjectId oId1_3 = insertAndAdd(points3);
        final ObjectId oId2_1 = insertAndAdd(lines1);

        {// simulate a commit so the repo head points to this new tree
            ObjectInserter objectInserter = repo.newObjectInserter();
            List<ObjectId> parents = ImmutableList.of();

            RevCommit commit = new CommitBuilder().setTreeId(newRepoTreeId1).setParentIds(parents)
                    .build();
            ObjectId commitId = commit.getId();
            objectInserter.insert(commit);
            Optional<Ref> newHead = geogit.command(UpdateRef.class).setName("refs/heads/master")
                    .setNewValue(commitId).call();
            assertTrue(newHead.isPresent());
        }

        final ObjectId newRepoTreeId2;
        {
            // write comparing the the previously generated tree instead of the repository HEAD, as
            // it was not updated (no commit op was performed)
            newRepoTreeId2 = geogit.command(WriteTree.class).setOldRoot(tree(newRepoTreeId1))
                    .call();

            // assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId2);

            System.err.println("++++++++ new root 2:" + newRepoTreeId2 + " ++++++++++");
            RevTree newRepoTree = repo.getTree(newRepoTreeId2);

            // check feature1_2, feature1_2 and feature2_1
            Optional<Node> treeChild;
            assertNotNull(treeChild = repo.getTreeChild(newRepoTree, appendChild(pointsName, idP2)));
            assertEquals(oId1_2, treeChild.get().getObjectId());

            assertNotNull(treeChild = repo.getTreeChild(newRepoTree, appendChild(pointsName, idP3)));
            assertEquals(oId1_3, treeChild.get().getObjectId());

            assertNotNull(treeChild = repo.getTreeChild(newRepoTree, appendChild(linesName, idL1)));
            assertEquals(oId2_1, treeChild.get().getObjectId());

            // as well as feature1_1 from the previous commit
            assertNotNull(treeChild = repo.getTreeChild(newRepoTree, appendChild(pointsName, idP1)));
            assertEquals(oId1_1, treeChild.get().getObjectId());
        }

        {// simulate a commit so the repo head points to this new tree
            ObjectInserter objectInserter = repo.newObjectInserter();
            List<ObjectId> parents = ImmutableList.of();
            RevCommit commit = new CommitBuilder().setTreeId(newRepoTreeId2).setParentIds(parents)
                    .build();
            ObjectId commitId = commit.getId();

            objectInserter.insert(commit);
            Optional<Ref> newHead = geogit.command(UpdateRef.class).setName("refs/heads/master")
                    .setNewValue(commitId).call();
            assertTrue(newHead.isPresent());
        }

        // delete feature1_1, feature1_3, and feature2_1
        assertTrue(deleteAndAdd(points1));
        assertTrue(deleteAndAdd(points3));
        assertTrue(deleteAndAdd(lines1));
        // and insert feature2_2
        final ObjectId oId2_2 = insertAndAdd(lines2);

        final ObjectId newRepoTreeId3;
        {
            // write comparing the the previously generated tree instead of the repository HEAD, as
            // it was not updated (no commit op was performed)
            newRepoTreeId3 = geogit.command(WriteTree.class).setOldRoot(tree(newRepoTreeId2))
                    .call();

            // assertEquals(index.getDatabase().getStagedRootRef().getObjectId(), newRepoTreeId3);

            System.err.println("++++++++ new root 3:" + newRepoTreeId3 + " ++++++++++");
            RevTree newRepoTree = repo.getTree(newRepoTreeId3);

            // and check only feature1_2 and feature2_2 remain
            assertFalse(repo.getTreeChild(newRepoTree, appendChild(pointsName, idP1)).isPresent());
            assertFalse(repo.getTreeChild(newRepoTree, appendChild(pointsName, idP3)).isPresent());
            assertFalse(repo.getTreeChild(newRepoTree, appendChild(linesName, idL3)).isPresent());

            assertEquals(oId1_2, repo.getTreeChild(newRepoTree, appendChild(pointsName, idP2))
                    .get().getObjectId());
            assertEquals(oId2_2, repo.getTreeChild(newRepoTree, appendChild(linesName, idL2)).get()
                    .getObjectId());
        }

    }

    @Test
    public void testAddEmptyTree() throws Exception {
        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        workingTree.createTypeTree(pointsName, pointsType);
        geogit.command(AddOp.class).setUpdateOnly(false).call();
        assertTrue(index.findStaged(pointsName).isPresent());
    }
}
