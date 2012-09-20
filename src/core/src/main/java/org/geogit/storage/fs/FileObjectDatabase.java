/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.fs;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.storage.AbstractObjectDatabase;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.inject.Inject;

public class FileObjectDatabase extends AbstractObjectDatabase implements ObjectDatabase {

    private final Platform platform;

    private final String databaseName;

    private File dataRoot;

    private String dataRootPath;

    @Inject
    public FileObjectDatabase(final Platform platform) {
        this(platform, "objects");
    }

    protected FileObjectDatabase(final Platform platform, final String databaseName) {
        checkNotNull(platform);
        checkNotNull(databaseName);
        this.platform = platform;
        this.databaseName = databaseName;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    protected File getDataRoot() {
        return dataRoot;
    }

    protected String getDataRootPath() {
        return dataRootPath;
    }

    @Override
    public void create() {
        final URL repoUrl = new ResolveGeogitDir(platform).call();
        if (repoUrl == null) {
            throw new IllegalStateException("Can't find geogit repository home");
        }

        try {
            dataRoot = new File(new File(repoUrl.toURI()), databaseName);
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }

        if (!dataRoot.exists() && !dataRoot.mkdirs()) {
            throw new IllegalStateException("Can't create environment: "
                    + dataRoot.getAbsolutePath());
        }
        if (!dataRoot.isDirectory()) {
            throw new IllegalStateException("Environment but is not a directory: "
                    + dataRoot.getAbsolutePath());
        }
        if (!dataRoot.canWrite()) {
            throw new IllegalStateException("Environment is not writable: "
                    + dataRoot.getAbsolutePath());
        }
        dataRootPath = dataRoot.getAbsolutePath();
    }

    @Override
    public boolean exists(final ObjectId id) {
        File f = filePath(id);
        return f.exists();
    }

    @Override
    protected InputStream getRawInternal(ObjectId id) {
        File f = filePath(id);
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * @see org.geogit.storage.AbstractObjectDatabase#putInternal(org.geogit.api.ObjectId, byte[])
     */
    @Override
    protected boolean putInternal(final ObjectId id, final byte[] rawData) {
        final File f = filePath(id);
        if (f.exists()) {
            return false;
        }

        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(f);
        } catch (FileNotFoundException dirDoesNotExist) {
            final File parent = f.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                throw new RuntimeException("Can't create " + parent.getAbsolutePath());
            }
            try {
                fileOutputStream = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                throw Throwables.propagate(e);
            }
        }
        try {
            fileOutputStream.write(rawData);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return true;
    }

    @Override
    public boolean delete(ObjectId objectId) {
        File filePath = filePath(objectId);
        boolean delete = filePath.delete();
        return delete;
    }

    private File filePath(final ObjectId id) {
        final String idName = id.toString();
        return filePath(idName);
    }

    private File filePath(final String objectId) {
        checkNotNull(objectId);
        checkArgument(objectId.length() > 4, "partial object id is too short");

        final char[] path1 = new char[2];
        final char[] path2 = new char[2];
        objectId.getChars(0, 2, path1, 0);
        objectId.getChars(2, 4, path2, 0);

        StringBuilder sb = new StringBuilder(dataRootPath);
        sb.append(File.separatorChar).append(path1).append(File.separatorChar).append(path2)
                .append(File.separatorChar).append(objectId);
        String filePath = sb.toString();
        return new File(filePath);
    }

    @Override
    public List<ObjectId> lookUp(final String partialId) {
        File parent = filePath(partialId).getParentFile();
        String[] list = parent.list();
        if (null == list) {
            return ImmutableList.of();
        }
        Builder<ObjectId> builder = ImmutableList.builder();
        for (String oid : list) {
            if (oid.startsWith(partialId)) {
                builder.add(ObjectId.valueOf(oid));
            }
        }
        return builder.build();
    }

    @Override
    protected List<ObjectId> lookUpInternal(byte[] raw) {
        throw new UnsupportedOperationException(
                "This method should not be called, we override lookUp(String) directly");
    }

}
