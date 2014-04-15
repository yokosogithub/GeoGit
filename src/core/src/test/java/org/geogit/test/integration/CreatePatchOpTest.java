/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.util.Iterator;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.Patch;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.CreatePatchOp;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.repository.WorkingTree;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;

public class CreatePatchOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testCreatePatch() throws Exception {
        insertAndAdd(points1, points2);
        geogit.command(CommitOp.class).setAll(true).call();

        final String featureId = points1.getIdentifier().getID();
        final Feature modifiedFeature = feature((SimpleFeatureType) points1.getType(), featureId,
                "changedProp", new Integer(1500), "POINT (2 2)");
        insert(modifiedFeature);
        insert(points3);
        delete(points2);

        Iterator<DiffEntry> diffs = geogit.command(DiffOp.class).call();
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();

        assertEquals(3, patch.count());
        assertEquals(1, patch.getAddedFeatures().size());
        assertEquals(1, patch.getRemovedFeatures().size());
        assertEquals(1, patch.getModifiedFeatures().size());
        assertEquals(RevFeatureType.build(pointsType), patch.getFeatureTypes().get(0));
        assertEquals(NodeRef.appendChild(pointsName, idP2), patch.getRemovedFeatures().get(0)
                .getPath());
        assertEquals(NodeRef.appendChild(pointsName, idP3), patch.getAddedFeatures().get(0)
                .getPath());

    }

    @Test
    public void testCreatePatchUsingIndex() throws Exception {
        insertAndAdd(points1, points2);
        geogit.command(CommitOp.class).setAll(true).call();

        final String featureId = points1.getIdentifier().getID();
        final Feature modifiedFeature = feature((SimpleFeatureType) points1.getType(), featureId,
                "changedProp", new Integer(1500), null);

        insertAndAdd(modifiedFeature);
        insertAndAdd(points3);
        deleteAndAdd(points2);
        delete(points3);
        DiffOp op = geogit.command(DiffOp.class);
        op.setCompareIndex(true);
        Iterator<DiffEntry> diffs = op.call();
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();

        assertEquals(3, patch.count());
        assertEquals(1, patch.getAddedFeatures().size());
        assertEquals(1, patch.getRemovedFeatures().size());
        assertEquals(1, patch.getModifiedFeatures().size());
        assertEquals(RevFeatureType.build(pointsType), patch.getFeatureTypes().get(0));
        assertEquals(NodeRef.appendChild(pointsName, idP2), patch.getRemovedFeatures().get(0)
                .getPath());
        assertEquals(NodeRef.appendChild(pointsName, idP3), patch.getAddedFeatures().get(0)
                .getPath());
    }

    @Test
    public void testCreatePatchWithNoChanges() throws Exception {
        insertAndAdd(points1, points2);
        geogit.command(CommitOp.class).setAll(true).call();
        Iterator<DiffEntry> diffs = geogit.command(DiffOp.class).call();
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();
        assertEquals(0, patch.count());
    }

    @Test
    public void testCreatePatchAddNewFeatureToEmptyRepo() throws Exception {
        insert(points1);
        DiffOp op = geogit.command(DiffOp.class);
        Iterator<DiffEntry> diffs = op.call();
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();
        assertEquals(1, patch.getAddedFeatures().size());
    }

    @Test
    public void testCreatePatchAddNewEmptyFeatureTypeToEmptyRepo() throws Exception {
        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        workingTree.createTypeTree(linesName, linesType);
        DiffOp op = geogit.command(DiffOp.class).setReportTrees(true);
        Iterator<DiffEntry> diffs = op.call();
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();
        assertEquals(1, patch.getAlteredTrees().size());
        assertEquals(ObjectId.NULL, patch.getAlteredTrees().get(0).getOldFeatureType());
        assertEquals(RevFeatureType.build(linesType).getId(), patch.getAlteredTrees().get(0)
                .getNewFeatureType());
        assertEquals(1, patch.getFeatureTypes().size());
    }

    @Test
    public void testCreatePatchRemoveEmptyFeatureType() throws Exception {
        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        workingTree.createTypeTree(linesName, linesType);
        geogit.command(AddOp.class).setUpdateOnly(false).call();
        workingTree.delete(linesName);
        DiffOp op = geogit.command(DiffOp.class).setReportTrees(true);
        Iterator<DiffEntry> diffs = op.call();
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();
        assertEquals(1, patch.getAlteredTrees().size());
        assertEquals(RevFeatureType.build(linesType).getId(), patch.getAlteredTrees().get(0)
                .getOldFeatureType());
        assertEquals(ObjectId.NULL, patch.getAlteredTrees().get(0).getNewFeatureType());
        assertEquals(1, patch.getFeatureTypes().size());
    }

    @Test
    public void testCreatePatchModifyFeatureType() throws Exception {
        DiffOp op = geogit.command(DiffOp.class).setReportTrees(true);

        insertAndAdd(points1, points2);
        geogit.getRepository().getWorkingTree().updateTypeTree(pointsName, modifiedPointsType);

        Iterator<DiffEntry> diffs = op.call();
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();
        assertEquals(1, patch.getAlteredTrees().size());
        assertEquals(RevFeatureType.build(pointsType).getId(), patch.getAlteredTrees().get(0)
                .getOldFeatureType());
        assertEquals(RevFeatureType.build(modifiedPointsType).getId(),
                patch.getAlteredTrees().get(0).getNewFeatureType());
        assertEquals(2, patch.getFeatureTypes().size());
    }

    @Test
    public void testCreatePatchAddNewEmptyPath() throws Exception {
        insert(points1);
        delete(points1);
        DiffOp op = geogit.command(DiffOp.class).setReportTrees(true);
        Iterator<DiffEntry> diffs = op.call();
        // ArrayList<DiffEntry> list = Lists.newArrayList(diffs);
        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(diffs).call();
        assertEquals(1, patch.getAlteredTrees().size());
    }

}
