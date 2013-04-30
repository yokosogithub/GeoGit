/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CloneOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.remote.RemoteRepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class ShallowCloneIntegrationTest extends RemoteRepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
    }

    private void createRepoAndShallowClone() throws Exception {
        // Commit several features to the remote

        LinkedList<RevCommit> expectedMaster = new LinkedList<RevCommit>();
        LinkedList<RevCommit> expectedBranch = new LinkedList<RevCommit>();

        insertAndAdd(remoteGeogit.geogit, points1);
        RevCommit commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);
        expectedBranch.addFirst(commit);

        // Create and checkout branch1
        remoteGeogit.geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("Branch1")
                .call();

        // Commit some changes to branch1
        insertAndAdd(remoteGeogit.geogit, points2);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedBranch.addFirst(commit);

        insertAndAdd(remoteGeogit.geogit, points3);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedBranch.addFirst(commit);

        // Make sure Branch1 has all of the commits
        Iterator<RevCommit> logs = remoteGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedBranch, logged);

        // Checkout master and commit some changes
        remoteGeogit.geogit.command(CheckoutOp.class).setSource("master").call();

        insertAndAdd(remoteGeogit.geogit, lines1);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);

        insertAndAdd(remoteGeogit.geogit, lines2);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);

        // Make sure master has all of the commits
        logs = remoteGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedMaster, logged);

        // Make sure the local repository has no commits prior to clone
        logs = localGeogit.geogit.command(LogOp.class).call();
        assertNotNull(logs);
        assertFalse(logs.hasNext());

        // clone from the remote
        CloneOp clone = clone();
        clone.setDepth(2);
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).call();

    }

    @Test
    public void testLogOp() throws Exception {
        createRepoAndShallowClone();
        Iterator<RevCommit> commits = localGeogit.geogit.command(LogOp.class).call();
        ArrayList<RevCommit> list = Lists.newArrayList(commits);
        assertEquals(2, list.size());
    }

    @Test
    public void testLogOpFilterSingleFeature() throws Exception {
        // LogOp with path restriction calls diff to see affected paths, so it should be checked in
        // the special case of a shallow clone

        createRepoAndShallowClone();

        String path = NodeRef.appendChild(linesName, lines1.getIdentifier().getID());

        List<RevCommit> feature2_1Commits = toList(localGeogit.geogit.command(LogOp.class)
                .addPath(path).call());
        assertEquals(1, feature2_1Commits.size());
    }

    @Test
    public void testRevParseAncestorBeyondDepth() throws Exception {
        createRepoAndShallowClone();
        Optional<ObjectId> revObject = remoteGeogit.geogit.command(RevParse.class)
                .setRefSpec("HEAD~2").call();
        assertTrue(revObject.isPresent());
        revObject = localGeogit.geogit.command(RevParse.class).setRefSpec("HEAD~2").call();
        assertFalse(revObject.isPresent());
    }

}
