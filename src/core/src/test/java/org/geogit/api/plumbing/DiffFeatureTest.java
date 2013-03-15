package org.geogit.api.plumbing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.google.common.base.Suppliers;

public class DiffFeatureTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        populate(true, points1);
        insert(points1_modified);
    }

    @Test
    public void testDiffBetweenEditedFeatures() {
        NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec("HEAD:" + NodeRef.appendChild(pointsName, idP1)).call();
        NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(NodeRef.appendChild(pointsName, idP1)).call();
        FeatureDiff diff = geogit.command(DiffFeature.class)
                .setOldVersion(Suppliers.ofInstance(oldRef))
                .setNewVersion(Suppliers.ofInstance(newRef)).call();
        assertTrue(diff.hasDifferences());
        System.out.println(diff);
    }

    @Test
    public void testDiffBetweenFeatureAndItself() {
        NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(NodeRef.appendChild(pointsName, idP1)).call();
        NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                .setRefspec(NodeRef.appendChild(pointsName, idP1)).call();
        FeatureDiff diff = geogit.command(DiffFeature.class)
                .setOldVersion(Suppliers.ofInstance(oldRef))
                .setNewVersion(Suppliers.ofInstance(newRef)).call();
        assertFalse(diff.hasDifferences());
        System.out.println(diff);
    }

    @Test
    public void testDiffUnexistentFeature() {
        try {
            NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class)
                    .setRefspec(NodeRef.appendChild(pointsName, "Points.100")).call();
            NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                    .setRefspec(NodeRef.appendChild(pointsName, idP1)).call();
            geogit.command(DiffFeature.class).setOldVersion(Suppliers.ofInstance(oldRef))
                    .setNewVersion(Suppliers.ofInstance(newRef)).call();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testDiffWrongPath() {
        try {
            NodeRef oldRef = geogit.command(FeatureNodeRefFromRefspec.class).setRefspec(pointsName).call();
            NodeRef newRef = geogit.command(FeatureNodeRefFromRefspec.class)
                    .setRefspec(NodeRef.appendChild(pointsName, idP1)).call();
            geogit.command(DiffFeature.class).setOldVersion(Suppliers.ofInstance(oldRef))
                    .setNewVersion(Suppliers.ofInstance(newRef)).call();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

}
