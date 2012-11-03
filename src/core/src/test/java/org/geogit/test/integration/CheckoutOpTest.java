/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
    public void testCheckoutCommitDettachedHead() throws Exception {
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        insertAndAdd(points2);
        final RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        insertAndAdd(lines1);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idL2).call();

        ObjectId workTreeId;
        workTreeId = geogit.command(CheckoutOp.class).setSource(c1.getId().toString()).call();
        assertEquals(c1.getTreeId(), workTreeId);

        assertFalse(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof Ref);

        workTreeId = geogit.command(CheckoutOp.class).setSource(c2.getId().toString()).call();
        assertEquals(c2.getTreeId(), workTreeId);

        workTreeId = geogit.command(CheckoutOp.class).setSource(c3.getId().toString()).call();
        assertEquals(c3.getTreeId(), workTreeId);
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

        ObjectId workTreeId;
        workTreeId = geogit.command(CheckoutOp.class).setSource("branch1").call();
        assertEquals(c1.getTreeId(), workTreeId);
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch1.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        workTreeId = geogit.command(CheckoutOp.class).setSource("branch2").call();
        assertEquals(c2.getTreeId(), workTreeId);
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch2.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());

        workTreeId = geogit.command(CheckoutOp.class).setSource("branch3").call();
        assertEquals(c3.getTreeId(), workTreeId);
        assertTrue(geogit.command(RefParse.class).setName(Ref.HEAD).call().get() instanceof SymRef);
        assertEquals(branch3.getName(), ((SymRef) geogit.command(RefParse.class).setName(Ref.HEAD)
                .call().get()).getTarget());
    }

}
