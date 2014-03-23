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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.BranchDeleteOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CloneOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.api.porcelain.MergeOp.MergeReport;
import org.geogit.api.porcelain.TagCreateOp;
import org.geogit.api.porcelain.TagListOp;
import org.geogit.remote.RemoteRepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.feature.Feature;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class CloneOpTest extends RemoteRepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testClone() throws Exception {
        // Commit several features to the remote
        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> expected = new LinkedList<RevCommit>();

        for (Feature f : features) {
            ObjectId oId = insertAndAdd(remoteGeogit.geogit, f);
            final RevCommit commit = remoteGeogit.geogit.command(CommitOp.class).call();
            expected.addFirst(commit);
            Optional<RevObject> childObject = remoteGeogit.geogit.command(RevObjectParse.class)
                    .setObjectId(oId).call();
            assertTrue(childObject.isPresent());
        }

        // Make sure the remote has all of the commits
        Iterator<RevCommit> logs = remoteGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);

        // Make sure the local repository has no commits prior to clone
        logs = localGeogit.geogit.command(LogOp.class).call();
        assertNotNull(logs);
        assertFalse(logs.hasNext());

        // clone from the remote
        CloneOp clone = clone();
        clone.setDepth(0);
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).call();

        // Make sure the local repository got all of the commits
        logs = localGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);
    }

    @Test
    public void testCloneWithTags() throws Exception {
        // Commit several features to the remote
        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> expected = new LinkedList<RevCommit>();
        List<RevTag> tags = Lists.newArrayList();

        for (Feature f : features) {
            ObjectId oId = insertAndAdd(remoteGeogit.geogit, f);
            final RevCommit commit = remoteGeogit.geogit.command(CommitOp.class).call();
            expected.addFirst(commit);
            Optional<RevObject> childObject = remoteGeogit.geogit.command(RevObjectParse.class)
                    .setObjectId(oId).call();
            assertTrue(childObject.isPresent());
            RevTag tag = remoteGeogit.geogit.command(TagCreateOp.class).setCommitId(commit.getId())
                    .setName(f.getIdentifier().getID()).call();
            tags.add(tag);
        }

        // Make sure the remote has all of the commits
        Iterator<RevCommit> logs = remoteGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);

        // Make sure the remote has all of the tags
        ImmutableList<RevTag> remoteTags = remoteGeogit.geogit.command(TagListOp.class).call();
        assertEquals(tags.size(), remoteTags.size());
        for (RevTag tag : tags) {
            assertTrue(remoteTags.contains(tag));
        }

        // Make sure the local repository has no commits prior to clone
        logs = localGeogit.geogit.command(LogOp.class).call();
        assertNotNull(logs);
        assertFalse(logs.hasNext());

        // clone from the remote
        CloneOp clone = clone();
        clone.setDepth(0);
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).call();

        // Make sure the local repository got all of the commits
        logs = localGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);

        /*
         * This is commented out, since the clone operation does not clone tags yet This test
         * verifies that no errors are raised when the repo to clone contains tags, but not to
         * verify that tags are also cloned, since that is not supported
         * 
         * I leave this dommented code here, to uncomment it once tag support is implemented for the
         * clone operation
         * 
         * 
         * // Make sure the local repository got all of the tags
         * 
         * ImmutableList<RevTag> localTags = localGeogit.geogit.command(TagListOp.class).call();
         * 
         * assertEquals(tags.size(), localTags.size());
         * 
         * for (RevTag tag : tags) {
         * 
         * assertTrue(localTags.contains(tag));
         * 
         * }
         */

    }

    @Test
    public void testCloneWithMergeCommit() throws Exception {
        // Commit several features to the remote

        LinkedList<RevCommit> expectedMaster = new LinkedList<RevCommit>();
        LinkedList<RevCommit> expectedBranch = new LinkedList<RevCommit>();

        insertAndAdd(remoteGeogit.geogit, points1);
        RevCommit commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);
        expectedBranch.addFirst(commit);

        insertAndAdd(remoteGeogit.geogit, points1_modified);
        commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);
        expectedBranch.addFirst(commit);

        // Create and checkout branch1
        remoteGeogit.geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName("Branch1")
                .call();

        // Commit a change to branch1
        insertAndAdd(remoteGeogit.geogit, points2);
        RevCommit branch1commit = remoteGeogit.geogit.command(CommitOp.class).call();
        expectedBranch.addFirst(branch1commit);

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

        // Merge branch1 into master
        MergeReport report = remoteGeogit.geogit.command(MergeOp.class)
                .addCommit(Suppliers.ofInstance(branch1commit.getId())).call();

        expectedMaster.addFirst(report.getMergeCommit());

        // Delete Branch1

        // Create and checkout branch1
        remoteGeogit.geogit.command(BranchDeleteOp.class).setName("Branch1").call();

        // clone from the remote
        CloneOp clone = clone();
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).setBranch("master").call();

        // Make sure the local repository got all of the commits
        logs = localGeogit.geogit.command(LogOp.class).setFirstParentOnly(true).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedMaster, logged);
    }

    @Test
    public void testShallowClone() throws Exception {
        // Commit several features to the remote
        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> expected = new LinkedList<RevCommit>();

        for (Feature f : features) {
            ObjectId oId = insertAndAdd(remoteGeogit.geogit, f);
            final RevCommit commit = remoteGeogit.geogit.command(CommitOp.class).call();
            expected.addFirst(commit);
            Optional<RevObject> childObject = remoteGeogit.geogit.command(RevObjectParse.class)
                    .setObjectId(oId).call();
            assertTrue(childObject.isPresent());
        }

        // Make sure the remote has all of the commits
        Iterator<RevCommit> logs = remoteGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);

        // Make sure the local repository has no commits prior to clone
        logs = localGeogit.geogit.command(LogOp.class).call();
        assertNotNull(logs);
        assertFalse(logs.hasNext());

        // clone from the remote
        CloneOp clone = clone();
        clone.setDepth(2);
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).call();

        // Make sure the local repository got only 2 commits
        logs = localGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(2, logged.size());
        assertEquals(expected.get(0), logged.get(0));
        assertEquals(expected.get(1), logged.get(1));
    }

    @Test
    public void testCloneRepoWithBranches() throws Exception {
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
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).setBranch("Branch1").call();

        // Make sure the local repository got all of the commits
        logs = localGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedBranch, logged);

        // Make sure the local master matches the remote
        localGeogit.geogit.command(CheckoutOp.class).setSource("master").call();

        logs = localGeogit.geogit.command(LogOp.class).call();
        logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expectedMaster, logged);
    }

    @Test
    public void testCloneEmptyRepo() throws Exception {
        CloneOp clone = clone();
        clone.setRepositoryURL(remoteGeogit.envHome.getCanonicalPath()).call();
    }

    @Test
    public void testCloneNoRepoSpecified() throws Exception {
        CloneOp clone = clone();
        exception.expect(IllegalArgumentException.class);
        clone.call();
    }

    @Test
    public void testCloneEmptyRepoString() throws Exception {
        CloneOp clone = clone();
        exception.expect(IllegalArgumentException.class);
        clone.setRepositoryURL("").call();
    }

}
