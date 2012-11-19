/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.StagingDatabase;

import com.google.inject.Inject;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;

/**
 * The Index (or Staging Area) object database.
 * <p>
 * This is a composite object database holding a reference to the actual repository object database
 * and a separate object database for the staging area itself.
 * <p>
 * Object look ups are first performed against the staging area database. If the object is not
 * found, then the look up is deferred to the actual repository database.
 * <p>
 * Object writes are always performed against the staging area object database.
 * <p>
 * The staging area database holds references to two root {@link RevTree trees}, one for the staged
 * objects and another one for the unstaged objects. When objects are added/changed/deleted to/from
 * the index, those modifications are written to the unstaged root tree. When objects are staged to
 * be committed, the unstaged objects are moved to the staged root tree.
 * <p>
 * A diff operation between the repository root tree and the index unstaged root tree results in the
 * list of unstaged objects.
 * <p>
 * A diff operation between the repository root tree and the index staged root tree results in the
 * list of staged objects.
 * 
 * @author groldan
 * 
 */
public class JEStagingDatabase implements ObjectDatabase, StagingDatabase {

    private final EnvironmentBuilder envProvider;

    // /////////////////////////////////////////
    /**
     * The staging area object database, contains only differences between the index and the
     * repository
     */
    private ObjectDatabase stagingDb;

    /**
     * The persistent repository objects. Lookup operations delegate to this one for any object not
     * found on the {@link #stagingDb}
     */
    private ObjectDatabase repositoryDb;

    /**
     * @param referenceDatabase the repository reference database, used to get the head re
     * @param repoDb
     * @param stagingDb
     */
    @Inject
    public JEStagingDatabase(final ObjectDatabase repositoryDb, final EnvironmentBuilder envBuilder) {
        this.repositoryDb = repositoryDb;
        this.envProvider = envBuilder;
    }

    @Override
    public boolean isOpen() {
        return stagingDb != null;
    }

    @Override
    public void open() {
        if (isOpen()) {
            return;
        }
        envProvider.setRelativePath("index");
        Environment environment = envProvider.get();
        stagingDb = new JEObjectDatabase(environment);
        stagingDb.open();
        {
            DatabaseConfig stagedDbConfig = new DatabaseConfig();
            stagedDbConfig.setAllowCreate(true);
            stagedDbConfig.setTransactional(environment.getConfig().getTransactional());
            // stagedDbConfig.setDeferredWrite(true);
            stagedDbConfig.setSortedDuplicates(false);
        }
    }

    @Override
    public void close() {
        if (stagingDb != null) {
            stagingDb.close();// this closes the environment since it took control over it
            stagingDb = null;
        }
    }

    // /////////////////////////////////////////////////////////////////////

    @Override
    public boolean exists(ObjectId id) {
        boolean exists = stagingDb.exists(id);
        if (!exists) {
            exists = repositoryDb.exists(id);
        }
        return exists;
    }

    @Override
    public InputStream getRaw(ObjectId id) {
        if (stagingDb.exists(id)) {
            return stagingDb.getRaw(id);
        }
        return repositoryDb.getRaw(id);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        Set<ObjectId> lookUp = new HashSet<ObjectId>(stagingDb.lookUp(partialId));
        lookUp.addAll(repositoryDb.lookUp(partialId));
        return new ArrayList<ObjectId>(lookUp);
    }

    @Override
    public <T> T get(ObjectId id, ObjectReader<T> reader) {
        if (stagingDb.exists(id)) {
            return stagingDb.get(id, reader);
        }
        return repositoryDb.get(id, reader);
    }

    @Override
    public boolean put(ObjectId id, ObjectWriter<?> writer) {
        return stagingDb.put(id, writer);
    }

    @Override
    public ObjectInserter newObjectInserter() {
        return stagingDb.newObjectInserter();
    }

    @Override
    public boolean delete(ObjectId objectId) {
        return stagingDb.delete(objectId);
    }

}
