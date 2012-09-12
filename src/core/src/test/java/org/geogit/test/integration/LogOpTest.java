/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.LogOp;
import org.geogit.repository.StagingArea;
import org.geotools.util.Range;
import org.junit.Test;
import org.opengis.feature.Feature;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class LogOpTest extends RepositoryTestCase {

    private LogOp logOp;

    private StagingArea index;

    @Override
    protected void setUpInternal() throws Exception {
        logOp = geogit.log();
        index = geogit.getRepository().getIndex();
    }

    @Test
    public void testEmptyRepo() throws Exception {
        Iterator<RevCommit> logs = logOp.call();
        assertNotNull(logs);
        assertFalse(logs.hasNext());
    }

    @Test
    public void testHeadWithSingleCommit() throws Exception {

        insertAndAdd(points1);
        final RevCommit firstCommit = geogit.commit().call();

        Iterator<RevCommit> iterator = logOp.call();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        assertEquals(firstCommit, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHeadWithTwoCommits() throws Exception {

        insertAndAdd(points1);
        final RevCommit firstCommit = geogit.commit().call();

        insertAndAdd(lines1);
        final RevCommit secondCommit = geogit.commit().call();

        Iterator<RevCommit> iterator = logOp.call();
        assertNotNull(iterator);

        assertTrue(iterator.hasNext());
        // by default returns most recent first
        assertEquals(secondCommit, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(firstCommit, iterator.next());

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHeadWithMultipleCommits() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> expected = new LinkedList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            final RevCommit commit = geogit.commit().call();
            expected.addFirst(commit);
        }

        Iterator<RevCommit> logs = logOp.call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }

        assertEquals(expected, logged);
    }

    @Test
    public void testPathFilterSingleFeature() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);

        RevCommit expectedCommit = null;

        for (Feature f : features) {
            insertAndAdd(f);
            String id = f.getIdentifier().getID();
            final RevCommit commit = geogit.commit().call();
            if (id.equals(lines1.getIdentifier().getID())) {
                expectedCommit = commit;
            }
        }

        String[] path = { linesName, lines1.getIdentifier().getID() };

        List<RevCommit> feature2_1Commits = toList(logOp.addPath(path).call());
        assertEquals(1, feature2_1Commits.size());
        assertEquals(Collections.singletonList(expectedCommit), feature2_1Commits);
    }

    @Test
    public void testPathFilterByTypeName() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        LinkedList<RevCommit> commits = Lists.newLinkedList();

        LinkedList<RevCommit> typeName1Commits = Lists.newLinkedList();

        for (Feature f : features) {
            insertAndAdd(f);
            final RevCommit commit = geogit.commit().setMessage(f.getIdentifier().toString())
                    .call();
            commits.addFirst(commit);
            if (pointsName.equals(f.getType().getName().getLocalPart())) {
                typeName1Commits.addFirst(commit);
            }
        }

        // path to filter commits on type1
        String[] path = { pointsName };

        List<RevCommit> logCommits = toList(logOp.addPath(path).call());
        assertEquals(typeName1Commits.size(), logCommits.size());
        assertEquals(typeName1Commits, logCommits);
    }

    @Test
    public void testLimit() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);

        for (Feature f : features) {
            insertAndAdd(f);
            geogit.commit().call();
        }

        assertEquals(3, Iterators.size(logOp.setLimit(3).call()));
        assertEquals(1, Iterators.size(logOp.setLimit(1).call()));
        assertEquals(4, Iterators.size(logOp.setLimit(4).call()));
    }

    @Test
    public void testTemporalConstraint() throws Exception {

        List<Feature> features = Arrays.asList(points1, lines1, points2, lines2, points3, lines3);
        List<Long> timestamps = Arrays.asList(Long.valueOf(1000), Long.valueOf(2000),
                Long.valueOf(3000), Long.valueOf(4000), Long.valueOf(5000), Long.valueOf(6000));

        LinkedList<RevCommit> allCommits = new LinkedList<RevCommit>();

        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);
            Long timestamp = timestamps.get(i);
            insertAndAdd(f);
            final RevCommit commit = geogit.commit().setTimestamp(timestamp).call();
            allCommits.addFirst(commit);
        }

        // test time range exclusive
        boolean minInclusive = false;
        boolean maxInclusive = false;
        Range<Date> commitRange = new Range<Date>(Date.class, new Date(2000), minInclusive,
                new Date(5000), maxInclusive);
        logOp.setTimeRange(commitRange);

        List<RevCommit> logged = toList(logOp.call());
        List<RevCommit> expected = allCommits.subList(2, 4);
        assertEquals(expected, logged);

        // test time range inclusive
        minInclusive = true;
        maxInclusive = true;
        commitRange = new Range<Date>(Date.class, new Date(2000), minInclusive, new Date(5000),
                maxInclusive);
        logOp = geogit.log().setTimeRange(commitRange);

        logged = toList(logOp.call());
        expected = allCommits.subList(1, 5);
        assertEquals(expected, logged);

        // test reset time range
        logOp = geogit.log().setTimeRange(commitRange).setTimeRange(null);
        logged = toList(logOp.call());
        expected = allCommits;
        assertEquals(expected, logged);
    }

    @Test
    public void testSinceUntil() throws Exception {
        final ObjectId oid1_1 = insertAndAdd(points1);
        final RevCommit commit1_1 = geogit.commit().call();

        final ObjectId oid1_2 = insertAndAdd(points2);
        final RevCommit commit1_2 = geogit.commit().call();

        final ObjectId oid2_1 = insertAndAdd(lines1);
        final RevCommit commit2_1 = geogit.commit().call();

        final ObjectId oid2_2 = insertAndAdd(lines2);
        final RevCommit commit2_2 = geogit.commit().call();

        try {
            logOp.setSince(oid1_1).call();
            fail("Expected ISE as since is not a commit");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("since"));
        }

        try {
            logOp.setSince(null).setUntil(oid2_2).call();
            fail("Expected ISE as until is not a commit");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("until"));
        }

        List<RevCommit> logs;
        List<RevCommit> expected;

        logs = toList(logOp.setSince(commit1_2.getId()).setUntil(null).call());
        expected = Arrays.asList(commit2_2, commit2_1);
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(commit2_2.getId()).setUntil(null).call());
        expected = Collections.emptyList();
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(commit1_2.getId()).setUntil(commit2_1.getId()).call());
        expected = Arrays.asList(commit2_1);
        assertEquals(expected, logs);

        logs = toList(logOp.setSince(null).setUntil(commit2_1.getId()).call());
        expected = Arrays.asList(commit2_1, commit1_2, commit1_1);
        assertEquals(expected, logs);
    }
}
