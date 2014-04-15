/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.merge.CheckMergeScenarioOp;
import org.geogit.api.plumbing.merge.MergeScenarioReport;
import org.geogit.api.plumbing.merge.ReportMergeScenarioOp;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.RemoveOp;
import org.geotools.data.DataUtilities;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.collect.Lists;

public class ReportMergeConflictsOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testAddedSameFeature() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        insertAndAdd(points2);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        insertAndAdd(points2);
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(0, conflicts.getConflicts().size());
        assertEquals(0, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertFalse(hasConflicts.booleanValue());
    }

    @Test
    public void testRemovedSameFeature() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        deleteAndAdd(points1);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        deleteAndAdd(points1);
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(0, conflicts.getConflicts().size());
        assertEquals(0, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertFalse(hasConflicts.booleanValue());
    }

    @Test
    public void testModifiedSameFeatureCompatible() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        Feature points1Modified = feature(pointsType, idP1, "StringProp1_2", new Integer(1000),
                "POINT(1 1)");
        insertAndAdd(points1Modified);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        Feature points1ModifiedB = feature(pointsType, idP1, "StringProp1_1", new Integer(2000),
                "POINT(1 1)");
        insertAndAdd(points1ModifiedB);
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(0, conflicts.getConflicts().size());
        assertEquals(0, conflicts.getUnconflicted().size());
        assertEquals(1, conflicts.getMerged().size());
        Feature pointsMerged = feature(pointsType, idP1, "StringProp1_2", new Integer(2000),
                "POINT(1 1)");
        assertEquals(pointsMerged, conflicts.getMerged().get(0).getFeature());
        Boolean hasConflictsOrAutomerge = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflictsOrAutomerge.booleanValue());

    }

    @Test
    public void testModifiedSameAttributeCompatible() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        Feature points1Modified = feature(pointsType, idP1, "StringProp1_2", new Integer(1000),
                "POINT(1 1)");
        insertAndAdd(points1Modified);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        Feature points1ModifiedB = feature(pointsType, idP1, "StringProp1_2", new Integer(2000),
                "POINT(1 1)");
        insertAndAdd(points1ModifiedB);
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(0, conflicts.getConflicts().size());
        assertEquals(1, conflicts.getUnconflicted().size());
        Boolean hasConflictsOrAutomerge = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflictsOrAutomerge.booleanValue());
    }

    @Test
    public void testModifiedSameFeatureIncompatible() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        Feature points1Modified = feature(pointsType, idP1, "StringProp1_2", new Integer(1000),
                "POINT(1 1)");
        insertAndAdd(points1Modified);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        Feature points1ModifiedB = feature(pointsType, idP1, "StringProp1_3", new Integer(1000),
                "POINT(1 1)");
        insertAndAdd(points1ModifiedB);
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(1, conflicts.getConflicts().size());
        assertEquals(0, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflicts.booleanValue());
    }

    @Test
    public void testModifiedAndRemoved() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        Feature points1Modified = feature(pointsType, idP1, "StringProp1_2", new Integer(1000),
                "POINT(1 1)");
        insertAndAdd(points1Modified);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        deleteAndAdd(points1);
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(1, conflicts.getConflicts().size());
        assertEquals(0, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflicts.booleanValue());
    }

    @Test
    public void testRemovedTreeOnlyInOneBranch() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        insertAndAdd(points2);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();

        geogit.command(RemoveOp.class).addPathToRemove(pointsName).call();
        geogit.command(AddOp.class).call();

        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(1, conflicts.getConflicts().size());
        assertEquals(1, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflicts.booleanValue());
    }

    @Test
    public void testAddedDifferentFeatures() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        insertAndAdd(points2);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        insertAndAdd(points3);
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(0, conflicts.getConflicts().size());
        assertEquals(1, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertFalse(hasConflicts.booleanValue());
    }

    @Test
    public void testAddedSameFeatureType() throws Exception {
        insertAndAdd(lines1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        insert(points2);
        delete(points2);
        geogit.command(AddOp.class).call();
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        insert(points2);
        delete(points2);
        geogit.command(AddOp.class).call();
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(0, conflicts.getConflicts().size());
        assertEquals(0, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertFalse(hasConflicts.booleanValue());
    }

    @Test
    public void testAddedDifferentFeatureType() throws Exception {
        insertAndAdd(lines1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        insert(points2);
        delete(points2);
        geogit.command(AddOp.class).call();
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        insert(points1B);
        delete(points1B);
        geogit.command(AddOp.class).call();
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(1, conflicts.getConflicts().size());
        assertEquals(0, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflicts.booleanValue());
    }

    @Test
    public void testModifiedDefaultFeatureTypeInBothBranches() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        geogit.getRepository().getWorkingTree().updateTypeTree(pointsName, modifiedPointsType);
        insert(points1B);
        geogit.command(AddOp.class).call();
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        String modifiedPointsTypeSpecB = "sp:String,ip:Integer,pp:Point:srid=4326,extraB:String";
        SimpleFeatureType modifiedPointsTypeB = DataUtilities.createType(pointsNs, pointsName,
                modifiedPointsTypeSpecB);
        geogit.getRepository().getWorkingTree().updateTypeTree(pointsName, modifiedPointsTypeB);
        insert(points1B);
        geogit.command(AddOp.class).call();
        RevCommit branchCommit = geogit.command(CommitOp.class).call();
        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(1, conflicts.getConflicts().size()); // the conflict in the feature type
        assertEquals(0, conflicts.getUnconflicted().size()); // the change in the feature is the
                                                             // same, so no conflict
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflicts.booleanValue());
    }

    @Test
    public void testModifiedFeatureTypeInOneBranchEditedAttributeValueInTheOther() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        insertAndAdd(points1_modified);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        insert(points1B);
        insert(points2);
        geogit.command(AddOp.class).call();
        RevCommit branchCommit = geogit.command(CommitOp.class).call();

        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(1, conflicts.getConflicts().size());
        assertEquals(1, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertTrue(hasConflicts.booleanValue());
    }

    @Test
    public void testModifiedFeatureTypeInOneBranch() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        geogit.command(BranchCreateOp.class).setName("TestBranch").call();
        insertAndAdd(points3);
        RevCommit masterCommit = geogit.command(CommitOp.class).call();
        geogit.command(CheckoutOp.class).setSource("TestBranch").call();
        insert(points1B);
        insert(points2);
        geogit.command(AddOp.class).call();
        RevCommit branchCommit = geogit.command(CommitOp.class).call();

        MergeScenarioReport conflicts = geogit.command(ReportMergeScenarioOp.class)
                .setMergeIntoCommit(masterCommit).setToMergeCommit(branchCommit).call();
        assertEquals(0, conflicts.getConflicts().size());
        assertEquals(2, conflicts.getUnconflicted().size());
        Boolean hasConflicts = geogit.command(CheckMergeScenarioOp.class)
                .setCommits(Lists.newArrayList(masterCommit, branchCommit)).call();
        assertFalse(hasConflicts.booleanValue());
    }

}
