/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.geogit.api.DiffEntry.ChangeType.ADD;
import static org.geogit.api.DiffEntry.ChangeType.DELETE;
import static org.geogit.api.DiffEntry.ChangeType.MODIFY;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.DiffTreeWalk;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.DiffOp;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Unit test suite for {@link DiffOp}, must cover {@link DiffTreeWalk} too.
 * 
 * @author groldan
 * 
 */
public class DiffOpFilterByIdTest extends RepositoryTestCase {

    private DiffOp diffOp;

    /**
     * records the addition of #points1 and #lines1
     */
    private RevCommit commit1;

    /**
     * records the addition of #points2 and #lines2
     */
    private RevCommit commit2;

    /**
     * records the delete of #points1 and the modification of #lines1
     */
    private RevCommit commit3;

    private ObjectId points2Id;

    private ObjectId lines2Id;

    private ObjectId commit1Id;

    private ObjectId commit2Id;

    private ObjectId commit3Id;

    private ObjectId points1Id;

    private ObjectId lines1Id;

    private ObjectId modifiedL1Id;

    private List<String> P1Path, P2Path, L1Path, L2Path;

    @Override
    protected void setUpInternal() throws Exception {
        this.diffOp = geogit.diff();

        points1Id = insertAndAdd(points1);
        lines1Id = insertAndAdd(lines1);
        commit1 = geogit.commit().setAll(true).call();

        points2Id = insertAndAdd(points2);
        lines2Id = insertAndAdd(lines2);
        commit2 = geogit.commit().setAll(true).call();

        deleteAndAdd(points1);
        ((SimpleFeature) lines1).setAttribute(0, "CHANGED");
        modifiedL1Id = insertAndAdd(lines1);// modifying a feature and inserting it has the same
                                            // effect
        commit3 = geogit.commit().setAll(true).call();

        commit1Id = commit1.getId();
        commit2Id = commit2.getId();
        commit3Id = commit3.getId();

        P1Path = Arrays.asList(pointsName, idP1);
        P2Path = Arrays.asList(pointsName, idP2);
        L1Path = Arrays.asList(linesName, idL1);
        L2Path = Arrays.asList(linesName, idL2);
    }

    @Test
    public void testFilterByObjectId1() throws Exception {
        diffOp.setOldVersion(commit1Id).setNewVersion(commit2Id);
        diffOp.setFilter(points2Id);
        List<DiffEntry> diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), ADD, commit1Id, commit2Id, ObjectId.NULL, points2Id, P2Path);
    }

    @Test
    public void testFilterByObjectId_NoToVersion() throws Exception {
        diffOp.setOldVersion(commit1Id);
        // newVersion shall resolve to current head, i.e., commit3
        diffOp.setFilter(points2Id);
        List<DiffEntry> diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), ADD, commit1Id, commit3Id, ObjectId.NULL, points2Id, P2Path);

        diffOp.setFilter(points1Id);
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), DELETE, commit1Id, commit3Id, points1Id, ObjectId.NULL, P1Path);

        diffOp.setFilter(modifiedL1Id);
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), MODIFY, commit1Id, commit3Id, lines1Id, modifiedL1Id, L1Path);
    }

    @Test
    public void testFilterByObjectId_InverseOrder() throws Exception {
        // set old and new version in inverse order than testFilterByObjectId_NoToVersion and expect
        // delete and add instead of add and delete
        diffOp.setOldVersion(commit3Id);
        diffOp.setNewVersion(commit1Id);

        diffOp.setFilter(points2Id);
        List<DiffEntry> diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), DELETE, commit3Id, commit1Id, points2Id, ObjectId.NULL, P2Path);

        diffOp.setFilter(points1Id);
        diffs = toList(diffOp.call());
        assertEquals(1, diffs.size());

        assertDiff(diffs.get(0), ADD, commit3Id, commit1Id, ObjectId.NULL, points1Id, P1Path);
    }

    private void assertDiff(final DiffEntry entry, final ChangeType changeType,
            final ObjectId oldCommitId, final ObjectId newCommitId, final ObjectId oldObjectId,
            final ObjectId newObjectId, final List<String> path) {

        assertEquals(changeType, entry.getType());
        assertEquals(oldCommitId, entry.getOldCommitId());
        assertEquals(newCommitId, entry.getNewCommitId());
        assertEquals(newObjectId, entry.getNewObjectId());
        assertEquals(oldObjectId, entry.getOldObjectId());
        assertEquals(path, entry.getPath());
    }
}
