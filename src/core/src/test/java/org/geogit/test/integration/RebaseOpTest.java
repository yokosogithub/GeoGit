/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.util.Iterator;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.LogOp;
import org.geogit.api.porcelain.RebaseOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

public class RebaseOpTest extends RepositoryTestCase {
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
    public void testRebase() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - master - Points 1 added
        // |\
        // | o - branch1 - Points 2 added
        // |
        // o - Points 3 added
        // |
        // o - branch2 - HEAD - Lines 1 added
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        final RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        // checkout master, then create branch2 and checkout
        geogit.command(CheckoutOp.class).setSource("master").call();
        final Ref branch2 = geogit.command(BranchCreateOp.class).setAutoCheckout(true)
                .setName("branch2").call();
        insertAndAdd(points3);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idP3).call();
        insertAndAdd(lines1);
        final RevCommit c4 = geogit.command(CommitOp.class).setMessage("commit for " + idL1).call();

        // Rebase branch2 onto branch1 to create the following revision graph
        // o
        // |
        // o - master - Points 1 added
        // |
        // o - branch1 - Points 2 added
        // |
        // o - Points 3 added
        // |
        // o - branch2 - HEAD - Lines 1 added

        Ref branch1 = geogit.command(RefParse.class).setName("branch1").call().get();
        geogit.command(RebaseOp.class).setUpstream(Suppliers.ofInstance(branch1.getObjectId()))
                .call();

        ObjectId workTreeId;
        workTreeId = geogit.command(CheckoutOp.class).setSource("branch1").call();
        assertTrue(c2.getTreeId().equals(workTreeId));
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch1.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        workTreeId = geogit.command(CheckoutOp.class).setSource("branch2").call();
        assertFalse(c4.getTreeId().equals(workTreeId));
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch2.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // Commit 4
        RevCommit logC4 = log.next();
        assertTrue(logC4.getAuthor().equals(c4.getAuthor()));
        assertTrue(logC4.getCommitter().equals(c4.getCommitter()));
        assertTrue(logC4.getMessage().equals(c4.getMessage()));
        assertFalse(logC4.getTimestamp() == c4.getTimestamp());
        assertFalse(logC4.getTreeId().equals(c4.getTreeId()));

        // Commit 3
        RevCommit logC3 = log.next();
        assertTrue(logC3.getAuthor().equals(c3.getAuthor()));
        assertTrue(logC3.getCommitter().equals(c3.getCommitter()));
        assertTrue(logC3.getMessage().equals(c3.getMessage()));
        assertFalse(logC3.getTimestamp() == c3.getTimestamp());
        assertFalse(logC3.getTreeId().equals(c3.getTreeId()));

        // Commit 2
        RevCommit logC2 = log.next();
        assertTrue(logC2.getAuthor().equals(c2.getAuthor()));
        assertTrue(logC2.getCommitter().equals(c2.getCommitter()));
        assertTrue(logC2.getMessage().equals(c2.getMessage()));
        assertTrue(logC2.getTimestamp() == c2.getTimestamp());
        assertTrue(logC2.getTreeId().equals(c2.getTreeId()));

        // Commit 1
        RevCommit logC1 = log.next();
        assertTrue(logC1.getAuthor().equals(c1.getAuthor()));
        assertTrue(logC1.getCommitter().equals(c1.getCommitter()));
        assertTrue(logC1.getMessage().equals(c1.getMessage()));
        assertTrue(logC1.getTimestamp() == c1.getTimestamp());
        assertTrue(logC1.getTreeId().equals(c1.getTreeId()));

    }

    @Test
    public void testRebaseOnto() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // |-o - Points 2 added
        // |-|
        // |-o - branch1 - Points 3 added
        // |--\
        // |---o - branch 2 - HEAD - Lines 1 added
        // |
        // o - master - Lines 2 added

        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();
        insertAndAdd(points3);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idP3).call();

        // create branch2 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch2").call();
        insertAndAdd(lines1);
        final RevCommit c4 = geogit.command(CommitOp.class).setMessage("commit for " + idL1).call();

        // checkout master and make a commit
        geogit.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(lines2);
        final RevCommit c5 = geogit.command(CommitOp.class).setMessage("commit for " + idL2).call();

        // checkout branch2
        geogit.command(CheckoutOp.class).setSource("branch2").call();

        // Rebase branch2 onto master to create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // | o - Points 2 added
        // | |
        // | o - branch1 - Points 3 added
        // |
        // o - master - Lines 2 added
        // |
        // o - branch 2 - HEAD - Lines 1 added

        Ref branch1 = geogit.command(RefParse.class).setName("branch1").call().get();
        Ref branch2 = geogit.command(RefParse.class).setName("branch2").call().get();
        Optional<Ref> master = geogit.command(RefParse.class).setName("master").call();

        geogit.command(RebaseOp.class).setUpstream(Suppliers.ofInstance(branch1.getObjectId()))
                .setOnto(Suppliers.ofInstance(master.get().getObjectId())).call();

        ObjectId workTreeId;
        workTreeId = geogit.command(CheckoutOp.class).setSource("branch1").call();
        assertTrue(c3.getTreeId().equals(workTreeId));
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch1.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        workTreeId = geogit.command(CheckoutOp.class).setSource("branch2").call();
        assertFalse(c4.getTreeId().equals(workTreeId));
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch2.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // Commit 3 -- Lines 1
        RevCommit logC3 = log.next();
        assertTrue(logC3.getAuthor().equals(c4.getAuthor()));
        assertTrue(logC3.getCommitter().equals(c4.getCommitter()));
        assertTrue(logC3.getMessage().equals(c4.getMessage()));
        assertFalse(logC3.getTimestamp() == c4.getTimestamp());
        assertFalse(logC3.getTreeId().equals(c4.getTreeId()));

        // Commit 2 -- Lines 2
        RevCommit logC2 = log.next();
        assertTrue(logC2.getAuthor().equals(c5.getAuthor()));
        assertTrue(logC2.getCommitter().equals(c5.getCommitter()));
        assertTrue(logC2.getMessage().equals(c5.getMessage()));
        assertTrue(logC2.getTimestamp() == c5.getTimestamp());
        assertTrue(logC2.getTreeId().equals(c5.getTreeId()));

        // Commit 1 -- Points 1
        RevCommit logC1 = log.next();
        assertTrue(logC1.getAuthor().equals(c1.getAuthor()));
        assertTrue(logC1.getCommitter().equals(c1.getCommitter()));
        assertTrue(logC1.getMessage().equals(c1.getMessage()));
        assertTrue(logC1.getTimestamp() == c1.getTimestamp());
        assertTrue(logC1.getTreeId().equals(c1.getTreeId()));
    }

    @Test
    public void testRebaseNoUpstream() throws Exception {
        exception.expect(IllegalStateException.class);
        geogit.command(RebaseOp.class).call();
    }

    @Test
    public void testRebaseNoCommits() throws Exception {
        Optional<Ref> master = geogit.command(RefParse.class).setName("master").call();
        exception.expect(IllegalStateException.class);
        geogit.command(RebaseOp.class)
                .setUpstream(Suppliers.ofInstance(master.get().getObjectId())).call();
    }

    @Test
    public void testRebaseNoUpstreamCommit() throws Exception {
        Optional<Ref> master = geogit.command(RefParse.class).setName("master").call();

        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        exception.expect(IllegalStateException.class);
        geogit.command(RebaseOp.class)
                .setUpstream(Suppliers.ofInstance(master.get().getObjectId())).call();
    }

}
