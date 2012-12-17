/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.geogit.api.NodeRef.appendChild;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.LogOp;
import org.geogit.api.porcelain.MergeOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Suppliers;

public class MergeOpTest extends RepositoryTestCase {
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
    public void testMerge() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // | o - branch1 - Points 2 added
        // |
        // o - Points 3 added
        // |
        // o - master - HEAD - Lines 1 added
        insertAndAdd(points1);
        final RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        final RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        // checkout master, then create branch2 and checkout
        geogit.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(points3);
        final RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for " + idP3).call();
        insertAndAdd(lines1);
        final RevCommit c4 = geogit.command(CommitOp.class).setMessage("commit for " + idL1).call();

        // Merge branch1 into master to create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // | o - branch1 - Points 2 added
        // | |
        // o | - Points 3 added
        // | |
        // o | - Lines 1 added
        // |/
        // o - master - HEAD - Merge commit

        Ref branch1 = geogit.command(RefParse.class).setName("branch1").call().get();
        final RevCommit mergeCommit = geogit.command(MergeOp.class)
                .addCommit(Suppliers.ofInstance(branch1.getObjectId()))
                .setMessage("My merge message.").call();

        RevTree mergedTree = repo.getTree(mergeCommit.getTreeId());

        String path = appendChild(pointsName, points2.getIdentifier().getID());
        assertTrue(repo.command(FindTreeChild.class).setParent(mergedTree).setChildPath(path)
                .call().isPresent());

        path = appendChild(pointsName, points1.getIdentifier().getID());
        assertTrue(repo.command(FindTreeChild.class).setParent(mergedTree).setChildPath(path)
                .call().isPresent());

        path = appendChild(pointsName, points3.getIdentifier().getID());
        assertTrue(repo.command(FindTreeChild.class).setParent(mergedTree).setChildPath(path)
                .call().isPresent());

        path = appendChild(linesName, lines1.getIdentifier().getID());
        assertTrue(repo.command(FindTreeChild.class).setParent(mergedTree).setChildPath(path)
                .call().isPresent());

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // Commit 4
        RevCommit logC4 = log.next();
        assertTrue(logC4.getMessage().equals("My merge message."));
        assertTrue(logC4.getParentIds().size() == 2);
        assertTrue(logC4.getParentIds().get(0).equals(c4.getId()));
        assertTrue(logC4.getParentIds().get(1).equals(c2.getId()));

        // Commit 3
        RevCommit logC3 = log.next();
        assertTrue(logC3.getAuthor().equals(c4.getAuthor()));
        assertTrue(logC3.getCommitter().equals(c4.getCommitter()));
        assertTrue(logC3.getMessage().equals(c4.getMessage()));
        assertTrue(logC3.getAuthor().getTimeZoneOffset().get().intValue() == c4.getAuthor()
                .getTimeZoneOffset().get().intValue());
        assertTrue(logC3.getAuthor().getTimestamp().get().longValue() == c4.getAuthor()
                .getTimestamp().get().longValue());
        assertTrue(logC3.getCommitter().getTimeZoneOffset().get().intValue() == c4.getCommitter()
                .getTimeZoneOffset().get().intValue());
        assertTrue(logC3.getCommitter().getTimestamp().get().longValue() == c4.getCommitter()
                .getTimestamp().get().longValue());
        assertTrue(logC3.getTreeId().equals(c4.getTreeId()));

        // Commit 2
        RevCommit logC2 = log.next();
        assertTrue(logC2.getAuthor().equals(c3.getAuthor()));
        assertTrue(logC2.getCommitter().equals(c3.getCommitter()));
        assertTrue(logC2.getMessage().equals(c3.getMessage()));
        assertTrue(logC2.getAuthor().getTimeZoneOffset().get().intValue() == c3.getAuthor()
                .getTimeZoneOffset().get().intValue());
        assertTrue(logC2.getAuthor().getTimestamp().get().longValue() == c3.getAuthor()
                .getTimestamp().get().longValue());
        assertTrue(logC2.getCommitter().getTimeZoneOffset().get().intValue() == c3.getCommitter()
                .getTimeZoneOffset().get().intValue());
        assertTrue(logC2.getCommitter().getTimestamp().get().longValue() == c3.getCommitter()
                .getTimestamp().get().longValue());
        assertTrue(logC2.getTreeId().equals(c3.getTreeId()));

        // Commit 1
        RevCommit logC1 = log.next();
        assertTrue(logC1.getAuthor().equals(c1.getAuthor()));
        assertTrue(logC1.getCommitter().equals(c1.getCommitter()));
        assertTrue(logC1.getMessage().equals(c1.getMessage()));
        assertTrue(logC1.getAuthor().getTimeZoneOffset().get().intValue() == c1.getAuthor()
                .getTimeZoneOffset().get().intValue());
        assertTrue(logC1.getAuthor().getTimestamp().get().longValue() == c1.getAuthor()
                .getTimestamp().get().longValue());
        assertTrue(logC1.getCommitter().getTimeZoneOffset().get().intValue() == c1.getCommitter()
                .getTimeZoneOffset().get().intValue());
        assertTrue(logC1.getCommitter().getTimestamp().get().longValue() == c1.getCommitter()
                .getTimestamp().get().longValue());
        assertTrue(logC1.getTreeId().equals(c1.getTreeId()));

    }
}
