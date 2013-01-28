package org.geogit.test.integration;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class AddOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testAddSingleFile() throws Exception {
        insert(points1);
        List<DiffEntry> diffs = toList(repo.getWorkingTree().getUnstaged(null));
        assertEquals(2, diffs.size());
        assertEquals(pointsName, diffs.get(0).newPath());
        assertEquals(NodeRef.appendChild(pointsName, idP1), diffs.get(1).newPath());
    }

    @Test
    public void testAddMultipleFeatures() throws Exception {
        insert(points1);
        insert(points2);
        insert(points3);
        geogit.command(AddOp.class).call();
        List<DiffEntry> unstaged = toList(repo.getWorkingTree().getUnstaged(null));
        assertEquals(ImmutableList.of(), unstaged);
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
    public void testAddMultipleFeaturesWithPathFilter() throws Exception {
        insert(points1);
        insert(points2);
        insert(lines1);
        geogit.command(AddOp.class).addPattern("Points").call();
        List<DiffEntry> unstaged = toList(repo.getWorkingTree().getUnstaged(null));
        assertEquals(2, unstaged.size());
        assertEquals(linesName, unstaged.get(0).newName());
        assertEquals(ChangeType.ADDED, unstaged.get(0).changeType());
        assertEquals(TYPE.TREE, unstaged.get(0).getNewObject().getType());
    }

    @Test
    public void testAddUpdate() throws Exception {
        insert(points1);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).call();

        insert(points1_modified);
        insert(lines1);
        geogit.command(AddOp.class).setUpdateOnly(true).call();
        List<DiffEntry> unstaged = toList(repo.getWorkingTree().getUnstaged(null));
        assertEquals(2, unstaged.size());
        assertEquals(linesName, unstaged.get(0).newName());
        assertEquals(lines1.getIdentifier().getID(), unstaged.get(1).newName());
    }

    @Test
    public void testAddUpdateWithPathFilter() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        insert(points1_modified);
        insert(lines1);

        // stage only Lines changed
        geogit.command(AddOp.class).setUpdateOnly(true).addPattern(pointsName).call();
        List<DiffEntry> staged = toList(repo.getIndex().getStaged(null));
        assertEquals(1, staged.size());
        assertEquals(idP1, staged.get(0).newName());

        List<DiffEntry> unstaged = toList(repo.getWorkingTree().getUnstaged(null));

        assertEquals(2, unstaged.size());
        assertEquals(linesName, unstaged.get(0).newName());
        assertEquals(idL1, unstaged.get(1).newName());

        geogit.command(AddOp.class).setUpdateOnly(true).addPattern("Points").call();
        unstaged = toList(repo.getWorkingTree().getUnstaged(null));

        assertEquals(2, unstaged.size());
        assertEquals(linesName, unstaged.get(0).newName());
        assertEquals(idL1, unstaged.get(1).newName());
    }

}
