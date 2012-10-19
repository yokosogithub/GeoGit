/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.test.integration.repository;

import static org.geogit.api.NodeRef.appendChild;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.hessian.GeoToolsRevFeature;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.util.NullProgressListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.feature.type.Name;

/**
 *
 */
public class WorkingTreeTest extends RepositoryTestCase {

    private WorkingTree workTree;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        workTree = repo.getWorkingTree();
    }

    @Test
    public void testInsertSingle() throws Exception {
        Name name = points1.getType().getName();
        String parentPath = name.getLocalPart();
        RevFeature rf = new GeoToolsRevFeature(points1);
        NodeRef ref = workTree.insert(parentPath, rf);
        ObjectId objectId = ref.getObjectId();

        assertEquals(objectId, workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollection() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        List<NodeRef> targetList = new LinkedList<NodeRef>();
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, 3);

        assertEquals(3, targetList.size());

        NodeRef ref1 = targetList.get(0);
        NodeRef ref2 = targetList.get(1);
        NodeRef ref3 = targetList.get(2);

        assertEquals(ref1.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(ref2.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(ref3.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollectionNullCollectionSize() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        List<NodeRef> targetList = new LinkedList<NodeRef>();
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, null);

        assertEquals(3, targetList.size());

        NodeRef ref1 = targetList.get(0);
        NodeRef ref2 = targetList.get(1);
        NodeRef ref3 = targetList.get(2);

        assertEquals(ref1.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(ref2.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(ref3.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollectionZeroCollectionSize() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        List<NodeRef> targetList = new LinkedList<NodeRef>();
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, 0);

        assertEquals(3, targetList.size());

        NodeRef ref1 = targetList.get(0);
        NodeRef ref2 = targetList.get(1);
        NodeRef ref3 = targetList.get(2);

        assertEquals(ref1.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(ref2.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(ref3.getObjectId(), workTree.findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
    }

    @Test
    public void testInsertCollectionNegativeCollectionSize() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        List<NodeRef> targetList = new LinkedList<NodeRef>();

        exception.expect(IllegalArgumentException.class);
        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                targetList, -5);
    }

    @Test
    public void testInsertCollectionNoTarget() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, null);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
    }

    @Test
    public void testUpdateFeatures() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        ObjectId oID1 = workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId();

        List<RevFeature> modifiedFeatures = new LinkedList<RevFeature>();
        modifiedFeatures.add(new GeoToolsRevFeature(points1_modified));

        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), 1);
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oID1));

    }

    @Test
    public void testUpdateFeaturesNullCollectionSize() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        ObjectId oID1 = workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId();

        List<RevFeature> modifiedFeatures = new LinkedList<RevFeature>();
        modifiedFeatures.add(new GeoToolsRevFeature(points1_modified));

        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), null);
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oID1));
    }

    @Test
    public void testUpdateFeaturesZeroCollectionSize() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        ObjectId oID1 = workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId();

        List<RevFeature> modifiedFeatures = new LinkedList<RevFeature>();
        modifiedFeatures.add(new GeoToolsRevFeature(points1_modified));

        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), 0);
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).get().getObjectId()
                .equals(oID1));
    }

    @Test
    public void testUpdateFeaturesNegativeCollectionSize() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        List<RevFeature> modifiedFeatures = new LinkedList<RevFeature>();
        modifiedFeatures.add(new GeoToolsRevFeature(points1_modified));

        exception.expect(IllegalArgumentException.class);
        workTree.update(pointsName, modifiedFeatures.iterator(), new NullProgressListener(), -5);
    }

    @Test
    public void testDeleteSingle() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId oldTreeId = workTree.getTree().getId();

        workTree.delete(pointsName, idP2);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId newTreeId = workTree.getTree().getId();

        assertFalse(oldTreeId.equals(newTreeId));
    }

    @Test
    public void testDeleteNonexistantFeature() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 2);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId oldTreeId = workTree.getTree().getId();

        workTree.delete(pointsName, idP3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        ObjectId newTreeId = workTree.getTree().getId();

        assertTrue(oldTreeId.equals(newTreeId));

    }

    @Test
    public void testDeleteCollection() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        List<RevFeature> deleteFeatures = new LinkedList<RevFeature>();
        deleteFeatures.add(new GeoToolsRevFeature(points1));
        deleteFeatures.add(new GeoToolsRevFeature(points3));

        Name typeName = points1.getName();

        workTree.delete(new QName(typeName.getNamespaceURI(), typeName.getLocalPart()), null,
                deleteFeatures.iterator());

        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
    }

    @Test
    public void testDeleteCollectionOfFeaturesNotPresent() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());

        List<RevFeature> deleteFeatures = new LinkedList<RevFeature>();
        deleteFeatures.add(new GeoToolsRevFeature(points3));

        Name typeName = points1.getName();

        workTree.delete(new QName(typeName.getNamespaceURI(), typeName.getLocalPart()), null,
                deleteFeatures.iterator());

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
    }

    @Test
    public void testDeleteFeatureType() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(lines1));
        featureList.add(new GeoToolsRevFeature(lines2));
        featureList.add(new GeoToolsRevFeature(lines3));

        workTree.insert(linesName, featureList.iterator(), false, new NullProgressListener(), null,
                3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL3)).isPresent());

        workTree.delete(new QName(pointsName));

        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL3)).isPresent());
    }

    @Test
    public void testHasRoot() throws Exception {
        insert(points1);
        Name typeName = points1.getName();
        assertFalse(workTree
                .hasRoot(new QName(typeName.getNamespaceURI(), typeName.getLocalPart())));
    }

    @Test
    public void testGetUnstaged() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        assertEquals(3, workTree.countUnstaged(null));

        Iterator<DiffEntry> changes = workTree.getUnstaged(null);

        assertNotNull(changes);
    }

    @Test
    public void testInsertMultipleFeatureTypes() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(lines1));
        featureList.add(new GeoToolsRevFeature(lines2));
        featureList.add(new GeoToolsRevFeature(lines3));

        workTree.insert(linesName, featureList.iterator(), false, new NullProgressListener(), null,
                3);

        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(pointsName, idP3)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL1)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL2)).isPresent());
        assertTrue(workTree.findUnstaged(appendChild(linesName, idL3)).isPresent());

    }

    @Test
    public void testGetFeatureTypeNames() throws Exception {
        List<RevFeature> featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(points1));
        featureList.add(new GeoToolsRevFeature(points2));
        featureList.add(new GeoToolsRevFeature(points3));

        workTree.insert(pointsName, featureList.iterator(), false, new NullProgressListener(),
                null, 3);

        featureList = new LinkedList<RevFeature>();
        featureList.add(new GeoToolsRevFeature(lines1));
        featureList.add(new GeoToolsRevFeature(lines2));
        featureList.add(new GeoToolsRevFeature(lines3));

        workTree.insert(linesName, featureList.iterator(), false, new NullProgressListener(), null,
                3);

        List<QName> featureTypes = workTree.getFeatureTypeNames();

        assertEquals(2, featureTypes.size());

        List<String> featureTypeNames = new LinkedList<String>();
        for (QName name : featureTypes) {
            featureTypeNames.add(name.getLocalPart());
        }

        assertTrue(featureTypeNames.contains(pointsName));
        assertTrue(featureTypeNames.contains(linesName));
    }
}
