package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;

import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.LsTreeOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;

public class LsFeaturesOpTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        populate(false, points1, points2, points3, lines1, lines2, lines3);
    }

    @Test
    public void testNonRecursiveRootListing() {
        Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).call();
        ArrayList<NodeRef> list = Lists.newArrayList(iter);
        assertEquals(list.size(), 0);
    }

    @Test
    public void testNonRecursiveTreeListing() {
        Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).setIncludeTrees(true).call();
        ArrayList<NodeRef> list = Lists.newArrayList(iter);
        assertEquals(list.size(), 2);
    }

    @Test
    public void testRecursiveRootListing() {
        Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).setRecursive(true).call();
        ArrayList<NodeRef> list = Lists.newArrayList(iter);
        assertEquals(list.size(), 6);
    }

    @Test
    public void testPathListing() {
        Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).setReference("Points").call();
        ArrayList<NodeRef> list = Lists.newArrayList(iter);
        assertEquals(list.size(), 3);
    }

    @Test
    public void testHEADNonRecursiveRootListing() {
        Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).setReference("HEAD").call();
        ArrayList<NodeRef> list = Lists.newArrayList(iter);
        assertEquals(list.size(), 0);
    }

    @Test
    public void testHEADNonRecursiveTreeListing() {
        Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).setReference("HEAD")
                .setIncludeTrees(true).call();
        ArrayList<NodeRef> list = Lists.newArrayList(iter);
        assertEquals(list.size(), 2);
    }

    @Test
    public void testUnexistentPathListing() {
        try {
            geogit.command(LsTreeOp.class).setReference("WORK_HEAD:WRONGPATH").call();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    // @Test
    // public void testHeadVsWorkHeadListing() throws Exception {
    // geogit.command(RemoveOp.class).addPathToRemove(pointsName).call();
    // Iterator<NodeRef> iterHead = geogit.command(LsFeaturesOp.class).setOrigin("HEAD").call();
    // Iterator<NodeRef> iterWorkHead = geogit.command(LsFeaturesOp.class).setOrigin("WORK_HEAD")
    // .call();
    // ArrayList<NodeRef> listHead = Lists.newArrayList(iterHead);
    // ArrayList<NodeRef> listWorkHead = Lists.newArrayList(iterWorkHead);
    // assertFalse(listHead.size() == listWorkHead.size());
    // }

    @Test
    public void testUnexistentOriginListing() {
        try {
            geogit.command(LsTreeOp.class).setReference("WRONGORIGIN").call();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

}
