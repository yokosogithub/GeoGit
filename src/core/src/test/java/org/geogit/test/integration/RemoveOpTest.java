package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.RemoveOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;

public class RemoveOpTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testSingleFeatureRemoval() throws Exception {

        populate(false, points1, points2, points3);

        String featureId = points1.getIdentifier().getID();
        String path = NodeRef.appendChild(pointsName, featureId);
        geogit.command(RemoveOp.class).addPathToRemove(path).call();

        List<DiffEntry> deleted = Lists.newArrayList(geogit.command(DiffWorkTree.class).call());

        // Check something has been deleted
        assertEquals(1, deleted.size());

        // Check the diffEntry corresponds to the deleted tree
        DiffEntry diffEntry = deleted.get(0);
        assertEquals(path, diffEntry.oldPath());
        assertNull(diffEntry.newPath());
    }

    @Test
    public void testMultipleRemoval() throws Exception {

        populate(false, points1, points2, points3);

        String featureId = points1.getIdentifier().getID();
        String path = NodeRef.appendChild(pointsName, featureId);
        String featureId2 = points2.getIdentifier().getID();
        String path2 = NodeRef.appendChild(pointsName, featureId2);

        geogit.command(RemoveOp.class).addPathToRemove(path).addPathToRemove(path2).call();

        List<DiffEntry> deleted = Lists.newArrayList(geogit.command(DiffWorkTree.class).call());
        assertEquals(2, deleted.size());
    }

    @Test
    public void testTreeRemoval() throws Exception {

        populate(false, points1, points2, points3, lines1, lines2);

        geogit.command(RemoveOp.class).addPathToRemove(pointsName).call();

        List<DiffEntry> deleted = Lists.newArrayList(geogit.command(DiffWorkTree.class).call());

        // Check that something has been deleted
        assertEquals(3, deleted.size());

    }

    @Test
    public void testUnexistentPathRemoval() throws Exception {

        populate(false, points1, points2, points3);

        try {
            geogit.command(RemoveOp.class).addPathToRemove("wrong/wrong.1").call();
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

}
