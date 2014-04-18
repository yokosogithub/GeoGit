package org.geogit.test.integration;

import org.geogit.api.porcelain.StatusOp;
import org.geogit.api.porcelain.StatusOp.StatusSummary;
import org.junit.Test;

public class StatusOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        super.populate(true, points1);
    }

    @Test
    public void testNothingToChange() {
        StatusSummary summary = geogit.command(StatusOp.class).call();
        assertAllFieldsNotNull(summary);
        assertEquals(0, summary.getCountStaged());
        assertEquals(0, summary.getCountUnstaged());
        assertEquals(0, summary.getCountConflicts());
    }

    @Test
    public void testOneAdd() {
        try {
            super.insert(points1_modified);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StatusSummary summary = geogit.command(StatusOp.class).call();
        assertAllFieldsNotNull(summary);
        assertEquals(1, summary.getCountUnstaged());
    }

    @Test
    public void testOneStaged() {
        try {
            super.insertAndAdd(points1_modified);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StatusSummary summary = geogit.command(StatusOp.class).call();
        assertAllFieldsNotNull(summary);
        assertEquals(1, summary.getCountStaged());
    }

    @Test
    public void testTwoStaged() {
        try {
            super.insert(points2);
            super.insertAndAdd(points1_modified);
        } catch (Exception e) {
            e.printStackTrace();
        }
        StatusSummary summary = geogit.command(StatusOp.class).call();
        assertAllFieldsNotNull(summary);
        assertEquals(2, summary.getCountStaged());
    }

    private void assertAllFieldsNotNull(StatusSummary summary) {
        assertNotNull(summary);
        assertNotNull(summary.getStaged());
        assertNotNull(summary.getUnstaged());
        assertNotNull(summary.getConflicts());
    }
}
