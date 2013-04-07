/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sleepycat.je.OperationStatus.NOTFOUND;
import static com.sleepycat.je.OperationStatus.SUCCESS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerializingFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.sleepycat.collections.CurrentTransaction;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;

/**
 * @TODO: extract interface
 */
public class JEObjectDatabase extends AbstractObjectDatabase implements ObjectDatabase {

    private EnvironmentBuilder envProvider;

    /**
     * Lazily loaded, do not access it directly but through {@link #getEnvironment()}
     */
    private Environment env;

    private Database objectDb;

    private ConfigDatabase configDb;

    @Nullable
    private CurrentTransaction txn;

    @Inject
    public JEObjectDatabase(final ObjectSerializingFactory serialFactory,
            final EnvironmentBuilder envProvider, final ConfigDatabase configDb) {
        super(serialFactory);
        checkNotNull(envProvider);
        checkNotNull(configDb);
        this.envProvider = envProvider;
        this.configDb = configDb;
    }

    public JEObjectDatabase(final ObjectSerializingFactory serialFactory, final Environment env,
            final ConfigDatabase configDb) {
        super(serialFactory);
        checkNotNull(env);
        checkNotNull(configDb);
        this.env = env;
        this.configDb = configDb;
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
            // env.evictMemory();
            env.cleanLog();
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

        dbConfig.setDeferredWrite(!transactional);

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
        cursorConfig.setReadUncommitted(true);

        Transaction transaction = txn == null ? null : txn.getTransaction();
        Cursor cursor = objectDb.openCursor(transaction, cursorConfig);
        try {
            // position cursor at the first closest key to the one looked up
            OperationStatus status = cursor.getSearchKeyRange(key, data, LockMode.READ_UNCOMMITTED);
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
                    status = cursor.getNext(key, data, LockMode.READ_UNCOMMITTED);
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

        final LockMode lockMode = LockMode.READ_UNCOMMITTED;
        Transaction transaction = txn == null ? null : txn.getTransaction();
        OperationStatus status = objectDb.get(transaction, key, data, lockMode);
        return SUCCESS == status;
    }

    @Override
    protected InputStream getRawInternal(final ObjectId id, final boolean failIfNotFound) {
        Preconditions.checkNotNull(id, "id");
        DatabaseEntry key = new DatabaseEntry(id.getRawValue());
        DatabaseEntry data = new DatabaseEntry();

        final LockMode lockMode = LockMode.READ_UNCOMMITTED;
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
    public void putAll(final Iterator<? extends RevObject> objects) {
        final String buffSizeKey = "je.bulkBuffer"; // @TODO: make this key a constant
        final int bulkBufferSize = getIntConfig(buffSizeKey, 2 * 1024 * 1024);

        final boolean transactional = this.objectDb.getConfig().getTransactional();
        Transaction transaction;
        TransactionConfig txConfig;
        final boolean handleTx;
        if (transactional) {
            txConfig = new TransactionConfig();
            txConfig.setDurability(Durability.COMMIT_NO_SYNC);
            txConfig.setReadUncommitted(true);
            transaction = txn.getTransaction();
            handleTx = transaction == null;
            if (handleTx) {
                transaction = txn.beginTransaction(txConfig);
            }
        } else {
            transaction = null;
            txConfig = null;
            handleTx = false;
        }

        // final int commitThreshold = 100 * 1000;
        // int count = 0;

        try {
            BulkLoadOutputStream rawOut = new BulkLoadOutputStream(bulkBufferSize);

            while (objects.hasNext()) {
                RevObject object = objects.next();
                try {
                    writeObject(object, rawOut);
                    rawOut.mark(object.getId());
                } catch (BufferOverflowException doDumpData) {
                    dumpBuffer(rawOut, transaction);
                    rawOut.reset();
                    writeObject(object, rawOut);
                    rawOut.mark(object.getId());
                }

                // count++;
                // if (count % commitThreshold == 0) {
                // env.evictMemory();
                // }
            }

            dumpBuffer(rawOut, transaction);
            rawOut.reset();
            rawOut = null;

            if (transactional) {
                if (handleTx) {
                    txn.commitTransaction();
                }
            } else {
                // finally force an environment checkpoint to ensure durability
                // Stopwatch sw = new Stopwatch().start();
                this.env.sync();
                // sw.stop();
                // System.err.printf("environment sync time %s\n", sw);
            }
        } catch (Exception e) {
            if (transactional) {
                txn.abortTransaction();
            }
            throw Throwables.propagate(e);
        }
    }

    private int getIntConfig(final String configKey, final int defaultValue) {
        Optional<String> configValue = configDb.get(configKey);
        if (!configValue.isPresent()) {
            configValue = configDb.getGlobal(configKey);
        }
        if (configValue.isPresent()) {
            try {
                return Integer.parseInt(configValue.get());
            } catch (NumberFormatException e) {
                System.err.printf("Config keyword %s is invalid: %s, using default value %d. %s\n",
                        configKey, configValue.get(), defaultValue, e.getMessage());
            }
        }
        return defaultValue;
    }

    private void dumpBuffer(BulkLoadOutputStream rawOut, Transaction transaction) {

        final byte[] rawData = rawOut.getBuffer();
        final SortedMap<ObjectId, int[]> sortedOffsets = rawOut.getSortedOffsets();

        if (sortedOffsets.isEmpty()) {
            return;
        }
        // System.err.printf("\n\tWriting %d objects in sorted order...", sortedOffsets.size());
        // Stopwatch sw = new Stopwatch().start();
        for (Map.Entry<ObjectId, int[]> objectOffset : sortedOffsets.entrySet()) {
            ObjectId objectId = objectOffset.getKey();
            int offset = objectOffset.getValue()[0];
            int length = objectOffset.getValue()[1];
            putInternal(objectId, rawData, offset, length, transaction);
        }
        // sw.stop();
        // System.err.printf(" done in %s.\n", sw);
    }

    @Override
    protected boolean putInternal(final ObjectId id, final byte[] rawData) {
        OperationStatus status;
        Transaction transaction = txn == null ? null : txn.getTransaction();
        status = putInternal(id, rawData, transaction);
        final boolean didntExist = SUCCESS.equals(status);

        return didntExist;
    }

    private OperationStatus putInternal(final ObjectId id, final byte[] rawData,
            Transaction transaction) {

        return putInternal(id, rawData, 0, rawData.length, transaction);
    }

    private OperationStatus putInternal(final ObjectId id, final byte[] rawData, int offset,
            int lenght, Transaction transaction) {

        OperationStatus status;
        final byte[] rawKey = id.getRawValue();
        DatabaseEntry key = new DatabaseEntry(rawKey);
        DatabaseEntry data = new DatabaseEntry(rawData, offset, lenght);

        status = objectDb.putNoOverwrite(transaction, key, data);
        return status;
    }

    @Override
    public boolean delete(final ObjectId id) {
        final byte[] rawKey = id.getRawValue();
        final DatabaseEntry key = new DatabaseEntry(rawKey);

        Transaction transaction = txn == null ? null : txn.getTransaction();
        final OperationStatus status = objectDb.delete(transaction, key);

        return SUCCESS.equals(status);
    }

    private final static class BulkLoadOutputStream extends ByteArrayOutputStream {

        private final int bufferSize;

        private int lastOffset;

        private TreeMap<ObjectId, int[]> sortedOffsets = Maps.newTreeMap();

        public BulkLoadOutputStream(int bufferSize) {
            super(bufferSize);
            this.bufferSize = bufferSize;
        }

        public byte[] getBuffer() {
            return super.buf;
        }

        private void checkBufferOverflow(int len) throws BufferOverflowException {
            if (size() + len > bufferSize) {
                throw new BufferOverflowException();
            }
        }

        public void mark(ObjectId oid) {
            int offset = getObjectOffset();
            int length = getObjectLength();
            this.sortedOffsets.put(oid, new int[] { offset, length });

            this.lastOffset = size();
        }

        public SortedMap<ObjectId, int[]> getSortedOffsets() {
            return this.sortedOffsets;
        }

        public int getObjectOffset() {
            return lastOffset;
        }

        public int getObjectLength() {
            return size() - lastOffset;
        }

        @Override
        public void reset() {
            this.lastOffset = 0;
            this.sortedOffsets.clear();
            super.reset();
        }

        @Override
        public synchronized void write(int b) throws BufferOverflowException {
            checkBufferOverflow(1);
            super.write(b);
        }

        @Override
        public synchronized void write(byte b[], int off, int len) throws BufferOverflowException {
            checkBufferOverflow(len);
            super.write(b, off, len);
        }
    }
}
