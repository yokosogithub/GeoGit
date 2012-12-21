/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CherryPickOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.LogOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Suppliers;

public class CherryPickOpTest extends RepositoryTestCase {
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
    public void testCherryPick() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - master - Points 1 added
        // .\
        // . o - Points 2 added
        // . |
        // . o - Points 3 added
        // . |
        // . o - Lines 1 added
        // . |
        // . o - branch1 - Lines 2 added
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        final RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();
        insertAndAdd(points3);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idP3).call();
        insertAndAdd(lines1);
        final RevCommit c4 = geogit.command(CommitOp.class).setMessage("commit for " + idL1).call();
        insertAndAdd(lines2);
        final RevCommit c5 = geogit.command(CommitOp.class).setMessage("commit for " + idL2).call();

        // Cherry pick several commits to create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |
        // o - Lines 2 added
        // |
        // o - Points 3 added
        // |
        // o - master - Points 2 added

        // switch back to master
        geogit.command(CheckoutOp.class).setSource("master").call();
        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.addCommit(Suppliers.ofInstance(c5.getId()));
        cherryPick.addCommit(Suppliers.ofInstance(c3.getId()));
        cherryPick.addCommit(Suppliers.ofInstance(c2.getId()));
        cherryPick.addCommit(Suppliers.ofInstance(c4.getId()));
        cherryPick.call();

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // Commit 5
        RevCommit logC5 = log.next();
        assertTrue(logC5.getAuthor().equals(c4.getAuthor()));
        assertTrue(logC5.getCommitter().equals(c4.getCommitter()));
        assertTrue(logC5.getMessage().equals(c4.getMessage()));
        assertFalse(logC5.getTimestamp() == c4.getTimestamp());
        assertFalse(logC5.getTreeId().equals(c4.getTreeId()));

        // Commit 4
        RevCommit logC4 = log.next();
        assertTrue(logC4.getAuthor().equals(c2.getAuthor()));
        assertTrue(logC4.getCommitter().equals(c2.getCommitter()));
        assertTrue(logC4.getMessage().equals(c2.getMessage()));
        assertFalse(logC4.getTimestamp() == c2.getTimestamp());
        assertFalse(logC4.getTreeId().equals(c2.getTreeId()));

        // Commit 3
        RevCommit logC3 = log.next();
        assertTrue(logC3.getAuthor().equals(c3.getAuthor()));
        assertTrue(logC3.getCommitter().equals(c3.getCommitter()));
        assertTrue(logC3.getMessage().equals(c3.getMessage()));
        assertFalse(logC3.getTimestamp() == c3.getTimestamp());
        assertFalse(logC3.getTreeId().equals(c3.getTreeId()));

        // Commit 2
        RevCommit logC2 = log.next();
        assertTrue(logC2.getAuthor().equals(c5.getAuthor()));
        assertTrue(logC2.getCommitter().equals(c5.getCommitter()));
        assertTrue(logC2.getMessage().equals(c5.getMessage()));
        assertFalse(logC2.getTimestamp() == c5.getTimestamp());
        assertFalse(logC2.getTreeId().equals(c5.getTreeId()));

        // Commit 1
        RevCommit logC1 = log.next();
        assertTrue(logC1.getAuthor().equals(c1.getAuthor()));
        assertTrue(logC1.getCommitter().equals(c1.getCommitter()));
        assertTrue(logC1.getMessage().equals(c1.getMessage()));
        assertTrue(logC1.getTimestamp() == c1.getTimestamp());
        assertTrue(logC1.getTreeId().equals(c1.getTreeId()));

        assertFalse(log.hasNext());

    }

    @Test
    public void testCherryPickInvalidCommit() throws Exception {
        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.addCommit(Suppliers.ofInstance(ObjectId.NULL));
        exception.expect(IllegalArgumentException.class);
        cherryPick.call();
    }

    @Test
    public void testCherryPickDirtyWorkTree() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        // checkout master and insert some features
        geogit.command(CheckoutOp.class).setSource("master").call();
        insert(points3);

        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.addCommit(Suppliers.ofInstance(c1.getId()));
        exception.expect(IllegalStateException.class);
        cherryPick.call();
    }

    @Test
    public void testCherryPickDirtyIndex() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        // checkout master and insert some features
        geogit.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(points3);

        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.addCommit(Suppliers.ofInstance(c1.getId()));
        exception.expect(IllegalStateException.class);
        cherryPick.call();
    }

    @Test
    public void testCherryPickRootCommit() throws Exception {
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        CherryPickOp cherryPick = geogit.command(CherryPickOp.class);
        cherryPick.addCommit(Suppliers.ofInstance(c1.getId()));
        cherryPick.call();

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // Commit 2
        RevCommit logC2 = log.next();
        assertTrue(logC2.getAuthor().equals(c1.getAuthor()));
        assertTrue(logC2.getCommitter().equals(c1.getCommitter()));
        assertTrue(logC2.getMessage().equals(c1.getMessage()));
        assertFalse(logC2.getTimestamp() == c1.getTimestamp());
        assertTrue(logC2.getTreeId().equals(c1.getTreeId()));

        // Commit 1
        RevCommit logC1 = log.next();
        assertTrue(logC1.getAuthor().equals(c1.getAuthor()));
        assertTrue(logC1.getCommitter().equals(c1.getCommitter()));
        assertTrue(logC1.getMessage().equals(c1.getMessage()));
        assertTrue(logC1.getTimestamp() == c1.getTimestamp());
        assertTrue(logC1.getTreeId().equals(c1.getTreeId()));

        assertFalse(log.hasNext());

    }

}
