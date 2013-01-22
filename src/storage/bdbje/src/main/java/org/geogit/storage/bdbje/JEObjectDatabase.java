/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import static com.sleepycat.je.OperationStatus.NOTFOUND;
import static com.sleepycat.je.OperationStatus.SUCCESS;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geotools.util.logging.Logging;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.sleepycat.collections.CurrentTransaction;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

/**
 * @TODO: extract interface
 */
public class JEObjectDatabase extends AbstractObjectDatabase implements ObjectDatabase {

    private static final Logger LOGGER = Logging.getLogger(JEObjectDatabase.class);

    private EnvironmentBuilder envProvider;

    /**
     * Lazily loaded, do not access it directly but through {@link #getEnvironment()}
     */
    private Environment env;

    private Database objectDb;

    @Nullable
    private CurrentTransaction txn;

    @Inject
    public JEObjectDatabase(final ObjectSerialisingFactory serialFactory,
            final EnvironmentBuilder envProvider) {
        super(serialFactory);
        this.envProvider = envProvider;
    }

    public JEObjectDatabase(final ObjectSerialisingFactory serialFactory, final Environment env) {
        super(serialFactory);
        this.env = env;
    }

    /**
     * @return the env
     */
    private synchronized Environment getEnvironment() {
        if (env == null) {
            env = envProvider.setRelativePath("objects").get();
        }
        return env;
    }

    @Override
    public void close() {
        // System.err.println("CLOSE");
        if (objectDb != null) {
            objectDb.close();
            objectDb = null;
        }
        if (env != null) {
            // System.err.println("--> " + env.getHome());
            env.evictMemory();
            env.sync();
            env.close();
            env = null;
        }
    }

    @Override
    public boolean isOpen() {
        return objectDb != null;
    }

    @Override
    public void open() {
        if (isOpen()) {
            return;
        }
        // System.err.println("OPEN");
        Environment environment = getEnvironment();
        // System.err.println("--> " + environment.getHome());
        txn = CurrentTransaction.getInstance(environment);

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        boolean transactional = getEnvironment().getConfig().getTransactional();
        dbConfig.setTransactional(transactional);
        Database database = environment.openDatabase(null, "ObjectDatabase", dbConfig);
        this.objectDb = database;
    }

    @Override
    protected List<ObjectId> lookUpInternal(final byte[] partialId) {

        DatabaseEntry key;
        {
            byte[] keyData = partialId.clone();
            key = new DatabaseEntry(keyData);
        }

        DatabaseEntry data = new DatabaseEntry();
        data.setPartial(0, 0, true);// do not retrieve data

        List<ObjectId> matches;

        CursorConfig cursorConfig = new CursorConfig();
        cursorConfig.setReadCommitted(true);
        cursorConfig.setReadUncommitted(false);

        Transaction transaction = txn == null ? null : txn.getTransaction();
        Cursor cursor = objectDb.openCursor(transaction, cursorConfig);
        try {
            // position cursor at the first closest key to the one looked up
            OperationStatus status = cursor.getSearchKeyRange(key, data, LockMode.DEFAULT);
            if (SUCCESS.equals(status)) {
                matches = new ArrayList<ObjectId>(2);
                final byte[] compKey = new byte[partialId.length];
                while (SUCCESS.equals(status)) {
                    byte[] keyData = key.getData();
                    System.arraycopy(keyData, 0, compKey, 0, compKey.length);
                    if (Arrays.equals(partialId, compKey)) {
                        matches.add(new ObjectId(keyData));
                    } else {
                        break;
                    }
                    status = cursor.getNext(key, data, LockMode.DEFAULT);
                }
            } else {
                matches = Collections.emptyList();
            }
            return matches;
        } finally {
            cursor.close();
        }
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#exists(org.geogit.api.ObjectId)
     */
    @Override
    public boolean exists(final ObjectId id) {
        Preconditions.checkNotNull(id, "id");

        DatabaseEntry key = new DatabaseEntry(id.getRawValue());
        DatabaseEntry data = new DatabaseEntry();
        // tell db not to retrieve data
        data.setPartial(0, 0, true);

        final LockMode lockMode = LockMode.DEFAULT;
        Transaction transaction = txn == null ? null : txn.getTransaction();
        OperationStatus status = objectDb.get(transaction, key, data, lockMode);
        return SUCCESS == status;
    }

    @Override
    protected InputStream getRawInternal(final ObjectId id, final boolean failIfNotFound) {
        Preconditions.checkNotNull(id, "id");
        DatabaseEntry key = new DatabaseEntry(id.getRawValue());
        DatabaseEntry data = new DatabaseEntry();

        final LockMode lockMode = LockMode.READ_COMMITTED;
        Transaction transaction = txn == null ? null : txn.getTransaction();
        OperationStatus operationStatus = objectDb.get(transaction, key, data, lockMode);
        if (NOTFOUND.equals(operationStatus)) {
            if (failIfNotFound) {
                throw new IllegalArgumentException("Object does not exist: " + id.toString());
            }
            return null;
        }
        final byte[] cData = data.getData();

        return new ByteArrayInputStream(cData);
    }

    @Override
    protected boolean putInternal(final ObjectId id, final byte[] rawData) {
        final byte[] rawKey = id.getRawValue();
        DatabaseEntry key = new DatabaseEntry(rawKey);
        DatabaseEntry data = new DatabaseEntry(rawData);

        OperationStatus status;
        Transaction transaction = txn == null ? null : txn.getTransaction();
        status = objectDb.putNoOverwrite(transaction, key, data);
        final boolean didntExist = SUCCESS.equals(status);

        if (LOGGER.isLoggable(Level.FINER)) {
            if (didntExist) {
                LOGGER.finer("Key already exists in blob store, blob reused for id: " + id);
            }
        }
        return didntExist;
    }

    @Override
    public boolean delete(final ObjectId id) {
        final byte[] rawKey = id.getRawValue();
        final DatabaseEntry key = new DatabaseEntry(rawKey);

        Transaction transaction = txn == null ? null : txn.getTransaction();
        final OperationStatus status = objectDb.delete(transaction, key);

        return SUCCESS.equals(status);
    }
}
