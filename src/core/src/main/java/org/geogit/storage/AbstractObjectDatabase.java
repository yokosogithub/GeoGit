/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.List;

import org.geogit.api.ObjectId;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;

public abstract class AbstractObjectDatabase implements ObjectDatabase {

    public AbstractObjectDatabase() {
        // TODO: use an external cache
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#lookUp(java.lang.String)
     */
    @Override
    public List<ObjectId> lookUp(final String partialId) {
        Preconditions.checkNotNull(partialId);

        byte[] raw = ObjectId.toRaw(partialId);

        return lookUpInternal(raw);
    }

    protected abstract List<ObjectId> lookUpInternal(byte[] raw);

    /**
     * @see org.geogit.storage.ObjectDatabase#get(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectReader)
     */
    @Override
    public <T> T get(final ObjectId id, final ObjectReader<T> reader) {
        Preconditions.checkNotNull(id, "id");
        Preconditions.checkNotNull(reader, "reader");

        T object;
        InputStream raw = getRaw(id);
        try {
            object = reader.read(id, raw);
        } finally {
            Closeables.closeQuietly(raw);
        }
        return object;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#getRaw(org.geogit.api.ObjectId)
     */
    @Override
    public InputStream getRaw(final ObjectId id) throws IllegalArgumentException {
        InputStream in = getRawInternal(id);
        try {
            return new LZFInputStream(in);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    protected abstract InputStream getRawInternal(ObjectId id) throws IllegalArgumentException;

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.storage.ObjectWriter)
     */
    @Override
    public final <T> ObjectId put(final ObjectWriter<T> writer) {
        MessageDigest sha1;
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        DigestOutputStream keyGenOut;
        try {
            sha1 = MessageDigest.getInstance("SHA1");

            keyGenOut = new DigestOutputStream(rawOut, sha1);
            // GZIPOutputStream cOut = new GZIPOutputStream(keyGenOut);
            LZFOutputStream cOut = new LZFOutputStream(keyGenOut);

            try {
                writer.write(cOut);
            } finally {
                // cOut.finish();
                cOut.flush();
                cOut.close();
                keyGenOut.flush();
                keyGenOut.close();
                rawOut.flush();
                rawOut.close();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        final byte[] rawData = rawOut.toByteArray();
        final byte[] rawKey = keyGenOut.getMessageDigest().digest();
        final ObjectId id = new ObjectId(rawKey);
        putInternal(id, rawData);
        return id;
    }

    /**
     * @see org.geogit.storage.ObjectDatabase#put(org.geogit.api.ObjectId,
     *      org.geogit.storage.ObjectWriter)
     */
    @Override
    public final boolean put(final ObjectId id, final ObjectWriter<?> writer) {
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        // GZIPOutputStream cOut = new GZIPOutputStream(rawOut);
        LZFOutputStream cOut = new LZFOutputStream(rawOut);
        try {
            // writer.write(cOut);
            writer.write(cOut);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            // cOut.finish();
            try {
                cOut.flush();
                cOut.close();
                rawOut.flush();
                rawOut.close();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        final byte[] rawData = rawOut.toByteArray();
        return putInternal(id, rawData);
    }

    /**
     * Stores the raw data for the given id <em>only if it does not exist</em> already, and returns
     * whether the object was actually inserted.
     */
    protected abstract boolean putInternal(ObjectId id, byte[] rawData);

    /**
     * @see org.geogit.storage.ObjectDatabase#newObjectInserter()
     */
    @Override
    public ObjectInserter newObjectInserter() {
        return new ObjectInserter(this);
    }
}
