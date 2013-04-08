/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.util.Collection;
import java.util.Map;

import org.geogit.api.NodeRef;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.BlameOp;
import org.geogit.api.porcelain.BlameReport;
import org.geogit.api.porcelain.CommitOp;
import org.junit.Test;
import org.opengis.feature.Feature;

public class BlameOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {

    }

    @Test
    public void testBlameChangedByASingleCommit() throws Exception {
        insertAndAdd(points1);
        RevCommit firstCommit = geogit.command(CommitOp.class).call();
        String path = NodeRef.appendChild(pointsName, idP1);
        BlameReport report = geogit.command(BlameOp.class).setPath(path).call();
        Map<String, RevCommit> changes = report.getChanges();
        assertEquals(3, changes.size());
        Collection<RevCommit> commits = changes.values();
        for (RevCommit commit : commits) {
            assertEquals(firstCommit, commit);
        }
    }

    @Test
    public void testBlameChangedByLastCommit() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        insertAndAdd(points1_modified);
        RevCommit secondCommit = geogit.command(CommitOp.class).call();
        String path = NodeRef.appendChild(pointsName, idP1);
        BlameReport report = geogit.command(BlameOp.class).setPath(path).call();
        Map<String, RevCommit> changes = report.getChanges();
        assertEquals(3, changes.size());
        Collection<RevCommit> commits = changes.values();
        for (RevCommit commit : commits) {
            assertEquals(secondCommit, commit);
        }
    }

    @Test
    public void testBlameChangedByTwoCommits() throws Exception {
        insertAndAdd(points1);
        RevCommit firstCommit = geogit.command(CommitOp.class).call();
        Feature pointsModified = feature(pointsType, idP1, "StringProp1_3", new Integer(1000),
                "POINT(1 1)");
        insertAndAdd(pointsModified);
        RevCommit secondCommit = geogit.command(CommitOp.class).call();
        String path = NodeRef.appendChild(pointsName, idP1);
        BlameReport report = geogit.command(BlameOp.class).setPath(path).call();
        Map<String, RevCommit> changes = report.getChanges();
        assertEquals(3, changes.size());
        assertEquals(changes.get("sp"), secondCommit);
        assertEquals(changes.get("ip"), firstCommit);
        assertEquals(changes.get("pp"), firstCommit);
    }

    @Test
    public void testBlameWithWrongFeaturePath() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        try {
            geogit.command(BlameOp.class).setPath("wrongpath").call();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("The supplied path does not exist"));
        }

    }

    @Test
    public void testBlameWithFeatureType() throws Exception {
        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        try {
            geogit.command(BlameOp.class).setPath(pointsName).call();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("The supplied path does not resolve to a feature"));
        }

    }

}