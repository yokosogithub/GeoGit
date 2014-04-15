/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CommitOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class BranchCreateOpTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testCreateBranch() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("Commit1").call();
        geogit.command(BranchCreateOp.class).setName("branch1").setAutoCheckout(true).call();

        Optional<Ref> branch1 = geogit.command(RefParse.class).setName("branch1").call();

        assertTrue(branch1.isPresent());

        Optional<Ref> master = geogit.command(RefParse.class).setName("master").call();

        assertEquals(master.get().getObjectId(), branch1.get().getObjectId());
    }

    @Test
    public void testNullNameForBranch() {
        exception.expect(IllegalStateException.class);
        geogit.command(BranchCreateOp.class).setName(null).call();
    }

    @Test
    public void testCreateBranchWithTheSameNameAsExistingBranch() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("Commit1").call();
        geogit.command(BranchCreateOp.class).setName("branch1").setAutoCheckout(true).call();

        exception.expect(IllegalArgumentException.class);
        geogit.command(BranchCreateOp.class).setName("branch1").call();
    }

    @Test
    public void testCreateBranchWithTheSameNameAsExistingBranchAndForce() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("Commit1").call();
        geogit.command(BranchCreateOp.class).setName("branch1").call();
        insertAndAdd(points2);
        RevCommit newCommit = geogit.command(CommitOp.class).setMessage("Commit2").call();
        geogit.command(BranchCreateOp.class).setName("branch1").setForce(true).call();
        Optional<Ref> branch1 = geogit.command(RefParse.class).setName("branch1").call();
        assertTrue(branch1.isPresent());
        assertEquals(branch1.get().getObjectId(), newCommit.getId());
    }

    @Test
    public void testCreateBranchFromMasterWithNoCommitsMade() {
        exception.expect(IllegalArgumentException.class);
        geogit.command(BranchCreateOp.class).setName("branch1").call();
    }

    @Test
    public void testCreateBranchFromBranchOtherThanMaster() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("Commit1").call();
        geogit.command(BranchCreateOp.class).setName("branch1").setAutoCheckout(false).call();

        insertAndAdd(points2);
        geogit.command(CommitOp.class).setMessage("Commit2").call();
        geogit.command(BranchCreateOp.class).setName("branch2").setAutoCheckout(true)
                .setSource("branch1").call();

        Optional<Ref> branch1 = geogit.command(RefParse.class).setName("branch1").call();

        assertTrue(branch1.isPresent());

        Optional<Ref> branch2 = geogit.command(RefParse.class).setName("branch2").call();

        assertTrue(branch2.isPresent());

        assertEquals(branch1.get().getObjectId(), branch2.get().getObjectId());
    }

    @Test
    public void testCreateBranchFromCommit() throws Exception {
        insertAndAdd(points1);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("Commit1").call();
        insertAndAdd(points2);
        geogit.command(CommitOp.class).setMessage("Commit2").call();
        insertAndAdd(points3);
        geogit.command(CommitOp.class).setMessage("Commit3").call();

        geogit.command(BranchCreateOp.class).setName("branch1").setAutoCheckout(true)
                .setSource(c1.getId().toString()).call();

        Optional<Ref> branch1 = geogit.command(RefParse.class).setName("branch1").call();

        assertTrue(branch1.isPresent());

        assertEquals(c1.getId(), branch1.get().getObjectId());

        Optional<Ref> master = geogit.command(RefParse.class).setName("master").call();

        assertFalse(master.get().getObjectId().equals(branch1.get().getObjectId()));
    }

    @Test
    public void testCreateBranchFromNonExistentCommit() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("Commit1").call();

        exception.expect(IllegalArgumentException.class);
        geogit.command(BranchCreateOp.class).setName("branch1").setAutoCheckout(true)
                .setSource("Nonexistent Commit").call();
    }

    @Test
    public void testCreateBranchFromSomethingOtherThanCommit() throws Exception {
        insertAndAdd(points1);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("Commit1").call();
        insertAndAdd(points2);
        geogit.command(CommitOp.class).setMessage("Commit2").call();
        insertAndAdd(points3);
        geogit.command(CommitOp.class).setMessage("Commit3").call();

        exception.expect(IllegalArgumentException.class);
        geogit.command(BranchCreateOp.class).setName("branch1").setAutoCheckout(true)
                .setSource(c1.getTreeId().toString()).call();
    }

    @Test
    public void testOrphan() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("Commit1").call();
        Ref branch1 = geogit.command(BranchCreateOp.class).setName("branch1").setAutoCheckout(true)
                .setOrphan(true).call();

        assertEquals(ObjectId.NULL, branch1.getObjectId());
        assertEquals(Ref.HEADS_PREFIX + "branch1", branch1.getName());
    }
}
