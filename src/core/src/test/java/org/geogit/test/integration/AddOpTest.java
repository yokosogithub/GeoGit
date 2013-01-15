package org.geogit.test.integration;

import java.util.Iterator;

import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.junit.Test;

public class AddOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testAddSingleFile() throws Exception {
        insert(points1);
        Iterator<DiffEntry> iterator = repo.getWorkingTree().getUnstaged(null);
        assertTrue(iterator.hasNext());
        DiffEntry entry = iterator.next();
        assertEquals(entry.newName(), points1.getIdentifier().getID());
        geogit.command(AddOp.class).call();
        iterator = repo.getWorkingTree().getUnstaged(null);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAddMultipleFiles() throws Exception {
        insert(points1);
        insert(points2);
        insert(points3);
        geogit.command(AddOp.class).call();
        Iterator<DiffEntry> iterator = repo.getWorkingTree().getUnstaged(null);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAddMultipleTimes() throws Exception {
        insert(points1);
        insert(points2);
        insert(points3);
        geogit.command(AddOp.class).call();
        Iterator<DiffEntry> iterator = repo.getWorkingTree().getUnstaged(null);
        assertFalse(iterator.hasNext());
        insert(lines1);
        insert(lines2);
        geogit.command(AddOp.class).call();
        iterator = repo.getWorkingTree().getUnstaged(null);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAddMultipleFilesWithPathFilter() throws Exception {
        insert(points1);
        insert(points2);
        insert(lines1);
        geogit.command(AddOp.class).addPattern("Points").call();
        Iterator<DiffEntry> iterator = repo.getWorkingTree().getUnstaged(null);
        assertTrue(iterator.hasNext());
        DiffEntry entry = iterator.next();
        assertEquals(lines1.getIdentifier().getID(), entry.newName());
    }

    @Test
    public void testAddUpdate() throws Exception {
        insert(points1);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).call();

        insert(points1_modified);
        insert(lines1);
        geogit.command(AddOp.class).setUpdateOnly(true).call();
        Iterator<DiffEntry> iterator = repo.getWorkingTree().getUnstaged(null);
        assertTrue(iterator.hasNext());
        DiffEntry entry = iterator.next();
        assertEquals(lines1.getIdentifier().getID(), entry.newName());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAddUpdateWithPathFilter() throws Exception {
        insert(points1);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).call();

        insert(points1_modified);
        insert(lines1);
        geogit.command(AddOp.class).setUpdateOnly(true).addPattern("Lines").call();
        Iterator<DiffEntry> iterator = repo.getWorkingTree().getUnstaged(null);
        assertTrue(iterator.hasNext());
        DiffEntry entry = iterator.next();
        assertEquals(lines1.getIdentifier().getID(), entry.newName());
        assertTrue(iterator.hasNext());
        entry = iterator.next();
        assertEquals(points1_modified.getIdentifier().getID(), entry.newName());

        geogit.command(AddOp.class).setUpdateOnly(true).addPattern("Points").call();
        iterator = repo.getWorkingTree().getUnstaged(null);
        assertTrue(iterator.hasNext());
        entry = iterator.next();
        assertEquals(lines1.getIdentifier().getID(), entry.newName());
        assertFalse(iterator.hasNext());
    }

}
