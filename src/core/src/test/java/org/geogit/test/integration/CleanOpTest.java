package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;

import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.CleanOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;

public class CleanOpTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testClean() throws Exception {

        insert(points1, points2, points3);

        geogit.command(CleanOp.class).call();
        Iterator<DiffEntry> deleted = geogit.command(DiffWorkTree.class).call();
        ArrayList<DiffEntry> list = Lists.newArrayList(deleted);
        // Check that all the features have been deleted
        assertEquals(list.size(), 0);
    }

    @Test
    public void testTreeClean() throws Exception {

        insert(points1, points2, points3, lines1);

        geogit.command(CleanOp.class).setPath(pointsName).call();
        Iterator<DiffEntry> deleted = geogit.command(DiffWorkTree.class).call();
        ArrayList<DiffEntry> list = Lists.newArrayList(deleted);
        // Check that all the point features have been deleted but not the line one
        assertEquals(list.size(), 1);

    }

    @Test
    public void testUnexistentPathRemoval() throws Exception {

        populate(false, points1, points2, points3);

        try {
            geogit.command(CleanOp.class).setPath(linesName).call();
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

    }

}
