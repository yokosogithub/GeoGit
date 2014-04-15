/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.api.plumbing.diff.FeatureTypeDiff;
import org.geogit.api.plumbing.diff.GenericAttributeDiffImpl;
import org.geogit.api.plumbing.diff.Patch;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.ApplyPatchOp;
import org.geogit.api.porcelain.CannotApplyPatchException;
import org.geogit.repository.WorkingTree;
import org.junit.Test;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ApplyPatchOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    private Optional<Node> findTreeChild(RevTree root, String pathRemove) {
        Optional<NodeRef> nodeRef = geogit.command(FindTreeChild.class).setParent(root)
                .setChildPath(pathRemove).setIndex(true).call();
        Optional<Node> node = Optional.absent();
        if (nodeRef.isPresent()) {
            node = Optional.of(nodeRef.get().getNode());
        }
        return node;
    }

    @Test
    public void testAddFeaturePatch() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        patch.addAddedFeature(path, points1, RevFeatureType.build(pointsType));
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        RevTree root = repo.getWorkingTree().getTree();
        assertNotNull(root);
        Optional<Node> typeTreeId = findTreeChild(root, pointsName);
        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);
        Optional<Node> featureBlobId = findTreeChild(root, path);
        assertTrue(featureBlobId.isPresent());
    }

    @Test
    public void testRemoveFeaturePatch() throws Exception {
        insert(points1);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        patch.addRemovedFeature(path, points1, RevFeatureType.build(pointsType));
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        RevTree root = repo.getWorkingTree().getTree();
        assertNotNull(root);
        Optional<Node> featureBlobId = findTreeChild(root, path);
        assertFalse(featureBlobId.isPresent());
    }

    @Test
    public void testModifyFeatureAttributePatch() throws Exception {
        insert(points1);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1.getProperty("sp").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, Optional.of("new"));
        map.put(pointsType.getDescriptor("sp"), diff);
        FeatureDiff feaureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(pointsType));
        patch.addModifiedFeature(feaureDiff);
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        RevTree root = repo.getWorkingTree().getTree();
        Optional<Node> featureBlobId = findTreeChild(root, path);
        assertTrue(featureBlobId.isPresent());
        Iterator<DiffEntry> unstaged = repo.getWorkingTree().getUnstaged(pointsName);
        ArrayList<DiffEntry> diffs = Lists.newArrayList(unstaged);
        assertEquals(2, diffs.size());
        Optional<RevFeature> feature = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:" + path).call(RevFeature.class);
        assertTrue(feature.isPresent());
        ImmutableList<Optional<Object>> values = feature.get().getValues();
        assertEquals("new", values.get(0).get());
    }

    @Test
    public void testModifyFeatureAttributeOutdatedPatch() throws Exception {
        insert(points1_modified);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1.getProperty("sp").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, Optional.of("new"));
        map.put(pointsType.getDescriptor("sp"), diff);
        FeatureDiff feaureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(pointsType));
        patch.addModifiedFeature(feaureDiff);
        try {
            geogit.command(ApplyPatchOp.class).setPatch(patch).call();
            fail();
        } catch (CannotApplyPatchException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testRemoveFeatureAttributePatch() throws Exception {
        insert(points1B);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1B.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1B.getProperty("extra").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, null);
        map.put(modifiedPointsType.getDescriptor("extra"), diff);
        FeatureDiff featureDiff = new FeatureDiff(path, map,
                RevFeatureType.build(modifiedPointsType), RevFeatureType.build(pointsType));
        patch.addModifiedFeature(featureDiff);
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        Optional<RevFeature> feature = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:" + path).call(RevFeature.class);
        assertTrue(feature.isPresent());
        ImmutableList<Optional<Object>> values = feature.get().getValues();
        assertEquals(points1.getProperties().size(), values.size());
        assertFalse(values.contains("ExtraString"));

    }

    @Test
    public void testAddFeatureAttributePatch() throws Exception {
        insert(points1);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> newValue = Optional.fromNullable(points1B.getProperty("extra").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(null, newValue);
        map.put(modifiedPointsType.getDescriptor("extra"), diff);
        FeatureDiff featureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(modifiedPointsType));
        patch.addModifiedFeature(featureDiff);
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        // TODO
    }

    @Test
    public void testRemoveFeatureAttributeOutdatedPatch() throws Exception {
        insert(points1B_modified);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1B.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1B.getProperty("extra").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, null);
        map.put(modifiedPointsType.getDescriptor("extra"), diff);
        FeatureDiff featureDiff = new FeatureDiff(path, map,
                RevFeatureType.build(modifiedPointsType), RevFeatureType.build(pointsType));
        patch.addModifiedFeature(featureDiff);
        try {
            geogit.command(ApplyPatchOp.class).setPatch(patch).call();
            fail();
        } catch (CannotApplyPatchException e) {
            assertTrue(true);
        }

    }

    @Test
    public void testAddFeatureAttributeOutdatedPatch() throws Exception {
        insert(points1B);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> newValue = Optional.fromNullable(points1B.getProperty("extra").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(null, newValue);
        map.put(modifiedPointsType.getDescriptor("extra"), diff);
        FeatureDiff featureDiff = new FeatureDiff(path, map,
                RevFeatureType.build(modifiedPointsType), RevFeatureType.build(modifiedPointsType));
        patch.addModifiedFeature(featureDiff);
        try {
            geogit.command(ApplyPatchOp.class).setPatch(patch).call();
            fail();
        } catch (CannotApplyPatchException e) {
            assertTrue(true);
        }

    }

    @Test
    public void testAddedFeatureExists() throws Exception {
        insert(points1);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        patch.addAddedFeature(path, points1, RevFeatureType.build(pointsType));
        try {
            geogit.command(ApplyPatchOp.class).setPatch(patch).call();
            fail();
        } catch (CannotApplyPatchException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testModifiedFeatureDoesNotExists() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1.getProperty("sp").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, Optional.of("new"));
        map.put(pointsType.getDescriptor("sp"), diff);
        FeatureDiff featureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(pointsType));
        patch.addModifiedFeature(featureDiff);
        try {
            geogit.command(ApplyPatchOp.class).setPatch(patch).call();
            fail();
        } catch (CannotApplyPatchException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testRemovedFeatureDoesNotExists() throws Exception {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        patch.addRemovedFeature(path, points1, RevFeatureType.build(pointsType));
        try {
            geogit.command(ApplyPatchOp.class).setPatch(patch).call();
            fail();
        } catch (CannotApplyPatchException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testPartialApplication() throws Exception {
        insert(points1, points2);
        Patch patch = new Patch();
        String pathRemove = NodeRef.appendChild(pointsName, points2.getIdentifier().getID());
        patch.addRemovedFeature(pathRemove, points2, RevFeatureType.build(pointsType));
        String pathModify = NodeRef.appendChild(pointsName, points1B.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1B.getProperty("extra").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, null);
        map.put(modifiedPointsType.getDescriptor("extra"), diff);
        FeatureDiff featureDiff = new FeatureDiff(pathModify, map,
                RevFeatureType.build(modifiedPointsType), RevFeatureType.build(pointsType));
        patch.addModifiedFeature(featureDiff);
        Patch rejected = geogit.command(ApplyPatchOp.class).setPatch(patch).setApplyPartial(true)
                .call();
        assertFalse(rejected.isEmpty());
        RevTree root = repo.getWorkingTree().getTree();
        assertNotNull(root);
        Optional<Node> featureBlobId = findTreeChild(root, pathRemove);
        assertFalse(featureBlobId.isPresent());
        // now we take the rejected patch and apply it, and the new rejected should be identical to
        // it
        Patch newRejected = geogit.command(ApplyPatchOp.class).setPatch(rejected)
                .setApplyPartial(true).call();
        assertEquals(rejected, newRejected);
    }

    @Test
    public void testApplyEmptyPatch() {
        Patch patch = new Patch();
        geogit.command(ApplyPatchOp.class).setPatch(patch).setApplyPartial(true).call();

    }

    @Test
    public void testReversedPatch() throws Exception {
        insert(points1, points2);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1.getProperty("sp").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, Optional.of("new"));
        map.put(pointsType.getDescriptor("sp"), diff);
        FeatureDiff feaureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(pointsType));
        patch.addModifiedFeature(feaureDiff);
        String removedPath = NodeRef.appendChild(pointsName, points2.getIdentifier().getID());
        patch.addRemovedFeature(removedPath, points2, RevFeatureType.build(pointsType));
        String addedPath = NodeRef.appendChild(pointsName, points3.getIdentifier().getID());
        patch.addAddedFeature(addedPath, points3, RevFeatureType.build(pointsType));
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        geogit.command(ApplyPatchOp.class).setPatch(patch.reversed()).call();
        RevTree root = repo.getWorkingTree().getTree();
        Optional<Node> featureBlobId = findTreeChild(root, removedPath);
        assertTrue(featureBlobId.isPresent());
        featureBlobId = findTreeChild(root, addedPath);
        assertFalse(featureBlobId.isPresent());
        Optional<RevFeature> feature = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:" + path).call(RevFeature.class);
        assertTrue(feature.isPresent());
        assertEquals(oldValue, feature.get().getValues().get(0));
    }

    @Test
    public void testAddEmptyFeatureTypePatch() throws Exception {
        Patch patch = new Patch();
        RevFeatureType featureType = RevFeatureType.build(pointsType);
        patch.addFeatureType(featureType);
        patch.addAlteredTree(new FeatureTypeDiff(pointsName, null, featureType.getId()));
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        RevTree root = repo.getWorkingTree().getTree();
        assertNotNull(root);
        Optional<Node> typeTreeId = findTreeChild(root, pointsName);
        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);
        assertEquals(featureType.getId(), typeTreeId.get().getMetadataId().get());
    }

    @Test
    public void testRemoveEmptyFeatureTypePatch() throws Exception {
        WorkingTree workingTree = geogit.getRepository().getWorkingTree();
        workingTree.createTypeTree(pointsName, pointsType);
        geogit.command(AddOp.class).setUpdateOnly(false).call();
        Patch patch = new Patch();
        RevFeatureType featureType = RevFeatureType.build(pointsType);
        patch.addFeatureType(featureType);
        patch.addAlteredTree(new FeatureTypeDiff(pointsName, featureType.getId(), null));
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        RevTree root = repo.getWorkingTree().getTree();
        assertNotNull(root);
        Optional<Node> typeTree = findTreeChild(root, pointsName);
        assertFalse(typeTree.isPresent());
    }

    @Test
    public void testModifiedFeatureType() throws Exception {
        insert(points2, points3, points1B);
        Patch patch = new Patch();
        RevFeatureType oldFeatureType = RevFeatureType.build(pointsType);
        RevFeatureType featureType = RevFeatureType.build(modifiedPointsType);
        patch.addFeatureType(featureType);
        patch.addAlteredTree(new FeatureTypeDiff(pointsName, oldFeatureType.getId(), featureType
                .getId()));
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        RevTree root = repo.getWorkingTree().getTree();
        assertNotNull(root);
        Optional<Node> typeTree = findTreeChild(root, pointsName);
        assertTrue(typeTree.isPresent());
        assertEquals(featureType.getId(), typeTree.get().getMetadataId().get());
        Optional<Node> featureNode = findTreeChild(root, NodeRef.appendChild(pointsName, idP2));
        assertTrue(featureNode.isPresent());
        assertEquals(oldFeatureType.getId(), featureNode.get().getMetadataId().get());
        featureNode = findTreeChild(root, NodeRef.appendChild(pointsName, idP1));
        assertTrue(featureNode.isPresent());
        assertFalse(featureNode.get().getMetadataId().isPresent());
    }

    @Test
    public void testAddFeatureWithNonDefaultFeatureType() throws Exception {
        insert(points2, points3);
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        patch.addAddedFeature(path, points1B, RevFeatureType.build(modifiedPointsType));
        geogit.command(ApplyPatchOp.class).setPatch(patch).call();
        RevTree root = repo.getWorkingTree().getTree();
        assertNotNull(root);
        Optional<Node> typeTreeId = findTreeChild(root, pointsName);
        assertEquals(typeTreeId.get().getMetadataId().get(), RevFeatureType.build(pointsType)
                .getId());
        RevTree typeTree = repo.getTree(typeTreeId.get().getObjectId());
        assertNotNull(typeTree);
        Optional<Node> featureBlobId = findTreeChild(root, path);
        assertEquals(RevFeatureType.build(modifiedPointsType).getId(), featureBlobId.get()
                .getMetadataId().orNull());
        assertTrue(featureBlobId.isPresent());
        path = NodeRef.appendChild(pointsName, points3.getIdentifier().getID());
        featureBlobId = findTreeChild(root, path);
        assertEquals(null, featureBlobId.get().getMetadataId().orNull());

    }

}
