/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutException;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CheckoutResult;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class CheckoutOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        // These values should be used during a commit to set author/committer
        // TODO: author/committer roles need to be defined better, but for
        // now they are the same thing.
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("groldan").call();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("groldan@opengeo.org").call();
    }

    @Test
    public void testAddPaths() throws Exception {
        ObjectId oID1 = insertAndAdd(points1);
        insertAndAdd(points2);
        insertAndAdd(points3);
        ObjectId oID2 = insertAndAdd(lines1);

        geogit.command(CommitOp.class).setMessage("commit for all features").call();

        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("testBranch").call();

        ObjectId oID1Modified = insertAndAdd(points1_modified);
        ObjectId oID3 = insertAndAdd(lines2);
        ObjectId oID4 = insertAndAdd(lines3);
        geogit.command(CommitOp.class).setMessage("commit for modified points1").call();

        List<String> paths = Arrays.asList(
                NodeRef.appendChild(pointsName, points1.getIdentifier().getID()),
                NodeRef.appendChild(linesName, lines1.getIdentifier().getID()));

        RevTree root = repo.getWorkingTree().getTree();

        Optional<Node> featureBlob1 = repo.getTreeChild(root, paths.get(0));
        assertEquals(oID1Modified, featureBlob1.get().getObjectId());

        Optional<Node> featureBlob2 = repo.getTreeChild(root, paths.get(1));
        assertEquals(oID2, featureBlob2.get().getObjectId());

        Optional<Node> featureBlob3 = repo.getTreeChild(root,
                NodeRef.appendChild(linesName, lines2.getIdentifier().getID()));
        assertEquals(oID3, featureBlob3.get().getObjectId());

        Optional<Node> featureBlob4 = repo.getTreeChild(root,
                NodeRef.appendChild(linesName, lines3.getIdentifier().getID()));
        assertEquals(oID4, featureBlob4.get().getObjectId());

        geogit.command(CheckoutOp.class).setSource("master").addPaths(paths).call();

        root = repo.getWorkingTree().getTree();

        featureBlob1 = repo.getTreeChild(root, paths.get(0));
        assertEquals(oID1, featureBlob1.get().getObjectId());

        featureBlob2 = repo.getTreeChild(root, paths.get(1));
        assertEquals(oID2, featureBlob2.get().getObjectId());

        featureBlob3 = repo.getTreeChild(root,
                NodeRef.appendChild(linesName, lines2.getIdentifier().getID()));
        assertEquals(oID3, featureBlob3.get().getObjectId());

        featureBlob4 = repo.getTreeChild(root,
                NodeRef.appendChild(linesName, lines3.getIdentifier().getID()));
        assertEquals(oID4, featureBlob4.get().getObjectId());

    }

    @Test
    public void testCheckoutCommitDettachedHead() throws Exception {
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        insertAndAdd(points2);
        final RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        insertAndAdd(lines1);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idL2).call();

        CheckoutResult result;
        result = geogit.command(CheckoutOp.class).setSource(c1.getId().toString()).call();
        assertEquals(c1.getTreeId(), result.getNewTree());

        assertFalse(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof Ref);

        result = geogit.command(CheckoutOp.class).setSource(c2.getId().toString()).call();
        assertEquals(c2.getTreeId(), result.getNewTree());

        result = geogit.command(CheckoutOp.class).setSource(c3.getId().toString()).call();
        assertEquals(c3.getTreeId(), result.getNewTree());
    }

    @Test
    public void testCheckoutBranch() throws Exception {
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();
        final Ref branch1 = geogit.command(BranchCreateOp.class).setName("branch1").call();

        insertAndAdd(points2);
        final RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();
        final Ref branch2 = geogit.command(BranchCreateOp.class).setName("branch2").call();

        insertAndAdd(lines1);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idL2).call();
        final Ref branch3 = geogit.command(BranchCreateOp.class).setName("branch3").call();

        CheckoutResult result;
        result = geogit.command(CheckoutOp.class).setSource("branch1").call();
        assertEquals(c1.getTreeId(), result.getNewTree());
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch1.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        result = geogit.command(CheckoutOp.class).setSource("branch2").call();
        assertEquals(c2.getTreeId(), result.getNewTree());
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch2.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        result = geogit.command(CheckoutOp.class).setSource("branch3").call();
        assertEquals(c3.getTreeId(), result.getNewTree());
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch3.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());
    }

    @Test
    public void testCheckoutPathFilter() throws Exception {
        ObjectId points1Id = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();
        insert(points1_modified);

        CheckoutResult result = geogit.command(CheckoutOp.class).addPath("Points/Points.1").call();

        Optional<RevTree> workTree = geogit.command(RevObjectParse.class)
                .setObjectId(result.getNewTree()).call(RevTree.class);

        Optional<NodeRef> nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.1").call();

        assertEquals(points1Id, nodeRef.get().getNode().getObjectId());
    }

    @Test
    public void testCheckoutPathFilterWithNothingInIndex() throws Exception {
        insertAndAdd(points2);
        insert(points1_modified);

        exception.expect(IllegalStateException.class);
        geogit.command(CheckoutOp.class).addPath("Points/Points.1").call();

    }

    @Test
    public void testCheckoutPathFilterWithMultiplePaths() throws Exception {
        ObjectId points1Id = insertAndAdd(points1);
        ObjectId lines1Id = insertAndAdd(lines1);
        geogit.command(CommitOp.class).setMessage("commit 1").call();
        insert(points1_modified);
        insert(lines2);
        Collection<String> paths = Arrays.asList("Points/Points.1", "Lines");
        CheckoutResult result = geogit.command(CheckoutOp.class).addPaths(paths).call();
        Optional<RevTree> workTree = geogit.command(RevObjectParse.class)
                .setObjectId(result.getNewTree()).call(RevTree.class);
        Optional<NodeRef> nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.1").call();

        assertEquals(points1Id, nodeRef.get().getNode().getObjectId());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Lines/Lines.1").call();

        assertEquals(lines1Id, nodeRef.get().getNode().getObjectId());
        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Lines/Lines.2").call();
        assertFalse(nodeRef.isPresent());
    }

    @Test
    public void testCheckoutPathFilterWithTreeOtherThanIndex() throws Exception {
        ObjectId points1Id = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit 1").call();
        ObjectId points2Id = insertAndAdd(points2);
        RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit 2").call();
        insertAndAdd(points3);
        geogit.command(CommitOp.class).setMessage("commit 3").call();
        insert(points1_modified);

        CheckoutResult result = geogit.command(CheckoutOp.class)
                .setSource(c2.getTreeId().toString()).addPath("Points").call();

        Optional<RevTree> workTree = geogit.command(RevObjectParse.class)
                .setObjectId(result.getNewTree()).call(RevTree.class);
        Optional<NodeRef> nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.1").call();

        assertEquals(points1Id, nodeRef.get().getNode().getObjectId());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.2").call();

        assertEquals(points2Id, nodeRef.get().getNode().getObjectId());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.3").call();

        assertFalse(nodeRef.isPresent());
    }

    @Test
    public void testCheckoutNoParametersSet() {
        exception.expect(IllegalStateException.class);
        geogit.command(CheckoutOp.class).call();
    }

    @Test
    public void testCheckoutBranchWithChangesInTheIndex() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();
        geogit.command(BranchCreateOp.class).setName("branch1").call();
        insertAndAdd(points2);
        exception.expect(CheckoutException.class);
        geogit.command(CheckoutOp.class).setSource("branch1").call();
    }

    @Test
    public void testCheckoutBranchWithChangesInTheWorkTree() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();
        geogit.command(BranchCreateOp.class).setName("branch1").call();
        insert(points2);
        exception.expect(CheckoutException.class);
        geogit.command(CheckoutOp.class).setSource("branch1").call();
    }

    @Test
    public void testCheckoutBranchWithForceOptionAndChangesInTheIndex() throws Exception {
        insertAndAdd(points1);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();
        Ref branch1 = geogit.command(BranchCreateOp.class).setName("branch1").call();
        insertAndAdd(points2);
        CheckoutResult result = geogit.command(CheckoutOp.class).setSource("branch1")
                .setForce(true).call();

        assertEquals(c1.getTreeId(), result.getNewTree());
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch1.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());
    }

    @Test
    public void testCheckoutPathFilterToUpdatePathThatIsntInIndex() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit 1").call();

        insertAndAdd(points2);
        geogit.command(CommitOp.class).setMessage("commit 2").call();

        insertAndAdd(points3);
        geogit.command(CommitOp.class).setMessage("commit 3").call();

        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();

        insertAndAdd(lines1);
        geogit.command(CommitOp.class).setMessage("commit 4").call();

        insertAndAdd(lines2);
        geogit.command(CommitOp.class).setMessage("commit 5").call();

        insertAndAdd(lines3);
        geogit.command(CommitOp.class).setMessage("commit 6").call();

        geogit.command(CheckoutOp.class).setSource("master").call();

        CheckoutResult result = geogit.command(CheckoutOp.class).setSource("branch1")
                .addPath("Lines/Lines.1").call();

        Optional<RevTree> workTree = geogit.command(RevObjectParse.class)
                .setObjectId(result.getNewTree()).call(RevTree.class);

        Optional<NodeRef> nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.1").call();
        assertTrue(nodeRef.isPresent());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.2").call();
        assertTrue(nodeRef.isPresent());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Points/Points.3").call();
        assertTrue(nodeRef.isPresent());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Lines/Lines.1").call();
        assertTrue(nodeRef.isPresent());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Lines/Lines.2").call();
        assertFalse(nodeRef.isPresent());

        nodeRef = geogit.command(FindTreeChild.class).setParent(workTree.get())
                .setChildPath("Lines/Lines.3").call();
        assertFalse(nodeRef.isPresent());
    }
}
