/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.integration.mongo;

import java.io.File;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.di.GeogitModule;
import org.geogit.storage.StagingDatabase;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class MongoConflictsTest extends RepositoryTestCase {

    @Rule
    public TemporaryFolder mockWorkingDirTempFolder = new TemporaryFolder();

    @Override
    public void setUpInternal() {
    }

    @Override
    protected Injector createInjector() {
        File workingDirectory;
        try {
            workingDirectory = mockWorkingDirTempFolder.getRoot();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        Platform testPlatform = new TestPlatform(workingDirectory);
        return Guice.createInjector(Modules.override(new GeogitModule()).with(
                new MongoTestStorageModule(), new TestModule(testPlatform)));
    }

    @Test
    public void testConflicts() {
        StagingDatabase db = geogit.getRepository().getIndex().getDatabase();

        List<Conflict> conflicts = db.getConflicts(null, null);
        assertTrue(conflicts.isEmpty());
        Conflict conflict = new Conflict(idP1, ObjectId.forString("ancestor"),
                ObjectId.forString("ours"), ObjectId.forString("theirs"));
        Conflict conflict2 = new Conflict(idP2,
                ObjectId.forString("ancestor2"), ObjectId.forString("ours2"),
                ObjectId.forString("theirs2"));
        db.addConflict(null, conflict);
        Optional<Conflict> returnedConflict = db.getConflict(null, idP1);
        assertTrue(returnedConflict.isPresent());
        assertEquals(conflict, returnedConflict.get());
        db.removeConflict(null, idP1);
        conflicts = db.getConflicts(null, null);
        assertTrue(conflicts.isEmpty());
        db.addConflict(null, conflict);
        db.addConflict(null, conflict2);
        assertEquals(2, db.getConflicts(null, null).size());
        db.removeConflicts(null);
        conflicts = db.getConflicts(null, null);
        assertTrue(conflicts.isEmpty());

        final String NS = "ns";
        db.addConflict(NS, conflict);
        db.addConflict(null, conflict2);
        returnedConflict = db.getConflict(NS, idP1);
        assertTrue(returnedConflict.isPresent());
        assertEquals(conflict, returnedConflict.get());
        assertEquals(1, db.getConflicts(NS, null).size());
        db.removeConflict(NS, idP1);
        conflicts = db.getConflicts(NS, null);
        assertTrue(conflicts.isEmpty());
        db.addConflict(NS, conflict);
        db.addConflict(NS, conflict2);
        assertEquals(2, db.getConflicts(NS, null).size());
        assertEquals(1, db.getConflicts(null, null).size());
        db.removeConflicts(NS);
        conflicts = db.getConflicts(NS, null);
        assertTrue(conflicts.isEmpty());
        conflicts = db.getConflicts(null, null);
        assertFalse(conflicts.isEmpty());
    }
}
