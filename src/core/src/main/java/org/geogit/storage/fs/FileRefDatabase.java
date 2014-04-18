<<<<<<< .merge_file_UclC8M
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage.fs;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.geogit.api.Ref.append;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.AbstractRefDatabase;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Provides an implementation of a GeoGit ref database that utilizes the file system for the storage
 * of refs.
 */
public class FileRefDatabase extends AbstractRefDatabase {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final Platform platform;

    private final ConfigDatabase configDB;

    /**
     * Constructs a new {@code FileRefDatabase} with the given platform.
     * 
     * @param platform the platform to use
     */
    @Inject
    public FileRefDatabase(Platform platform, ConfigDatabase configDB) {
        this.platform = platform;
        this.configDB = configDB;
    }

    /**
     * Creates the reference database.
     */
    @Override
    public void create() {
        Optional<URL> envHome = new ResolveGeogitDir(platform).call();
        checkState(envHome.isPresent(), "Not inside a geogit directory");

        final URL envURL = envHome.get();
        if (!"file".equals(envURL.getProtocol())) {
            throw new UnsupportedOperationException(
                    "This References Database works only against file system repositories. "
                            + "Repository location: " + envURL.toExternalForm());
        }
        File repoDir;
        try {
            repoDir = new File(envURL.toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        File refs = new File(repoDir, "refs");
        if (!refs.exists() && !refs.mkdir()) {
            throw new IllegalStateException("Cannot create refs directory '"
                    + refs.getAbsolutePath() + "'");
        }
    }

    /**
     * Closes the reference database.
     */
    @Override
    public void close() {
        // nothing to close
    }

    /**
     * @param name the name of the ref (e.g. {@code "refs/remotes/origin"}, etc).
     * @return the ref, or {@code null} if it doesn't exist
     */
    @Override
    public String getRef(String name) {
        checkNotNull(name);
        File refFile = toFile(name);
        if (!refFile.exists() || refFile.isDirectory()) {
            return null;
        }
        String value = readRef(refFile);
        if (value == null) {
            return null;
        }
        try {
            ObjectId.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        return value;
    }

    /**
     * @param name the name of the symbolic ref (e.g. {@code "HEAD"}, etc).
     * @return the ref, or {@code null} if it doesn't exist
     */
    @Override
    public String getSymRef(String name) {
        checkNotNull(name);
        File refFile = toFile(name);
        if (!refFile.exists() || refFile.isDirectory()) {
            return null;
        }
        String value = readRef(refFile);
        if (value == null) {
            return null;
        }
        if (!value.startsWith("ref: ")) {
            throw new IllegalArgumentException(name + " is not a symbolic ref: '" + value + "'");
        }
        return value.substring("ref: ".length());
    }

    /**
     * @param refName the name of the ref
     * @param refValue the value of the ref
     * @return {@code null} if the ref didn't exist already, its old value otherwise
     */
    @Override
    public void putRef(String refName, String refValue) {
        checkNotNull(refName);
        checkNotNull(refValue);
        try {
            ObjectId.forString(refValue);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        store(refName, refValue);
    }

    /**
     * @param name the name of the symbolic ref
     * @param val the value of the symbolic ref
     * @return {@code null} if the ref didn't exist already, its old value otherwise
     */
    @Override
    public void putSymRef(String name, String val) {
        checkNotNull(name);
        checkNotNull(val);
        checkArgument(!name.equals(val), "Trying to store cyclic symbolic ref: %s", name);
        val = "ref: " + val;
        store(name, val);
    }

    /**
     * @param refName the name of the ref to remove (e.g. {@code "HEAD"},
     *        {@code "refs/remotes/origin"}, etc).
     * @return the value of the ref before removing it, or {@code null} if it didn't exist
     */
    @Override
    public String remove(String refName) {
        checkNotNull(refName);
        File refFile = toFile(refName);
        String oldRef;
        if (refFile.exists()) {
            oldRef = readRef(refFile);
            if (!refFile.delete()) {
                throw new RuntimeException("Unable to delete ref file '"
                        + refFile.getAbsolutePath() + "'");
            }
        } else {
            oldRef = null;
        }
        return oldRef;
    }

    /**
     * @param refPath
     * @return
     */
    private File toFile(String refPath) {
        Optional<URL> envHome = new ResolveGeogitDir(platform).call();

        String[] path = refPath.split("/");

        try {
            File file = new File(envHome.get().toURI());
            for (String subpath : path) {
                file = new File(file, subpath);
            }
            return file;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String readRef(final File refFile) {
        try {
            synchronized (refFile.getCanonicalPath().intern()) {
                return Files.readFirstLine(refFile, CHARSET);
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void store(String refName, String refValue) {
        final File refFile = toFile(refName);
        final File tmpFile = new File(refFile.getParentFile(), "." + refFile.getName() + ".tmp");

        try {
            Files.createParentDirs(tmpFile);
            Files.write(refValue + "\n", tmpFile, CHARSET);
            boolean renamed;
            synchronized (refFile.getCanonicalPath().intern()) {
                refFile.delete();
                renamed = tmpFile.renameTo(refFile);
            }
            checkState(renamed, "unable to save ref " + refName);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * @return all known references under the "refs" namespace (i.e. not top level ones like HEAD,
     *         etc), key'ed by ref name
     */
    @Override
    public Map<String, String> getAll() {
        return getAll("refs");
    }

    /**
     * @return all references under the specified namespace
     */
    @Override
    public Map<String, String> getAll(String namespace) {
        File refsRoot;
        try {
            Optional<URL> envHome = new ResolveGeogitDir(platform).call();
            refsRoot = new File(envHome.get().toURI());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        if (namespace.endsWith("/")) {
            namespace = namespace.substring(0, namespace.length() - 1);
        }
        Map<String, String> refs = Maps.newTreeMap();
        findRefs(refsRoot, namespace, refs);
        return ImmutableMap.copyOf(refs);
    }

    private void findRefs(final File refsRoot, final String namespace,
            final Map<String, String> target) {
        String[] subdirs = namespace.split("/");
        File nsDir = refsRoot;
        for (String subdir : subdirs) {
            nsDir = new File(nsDir, subdir);
            if (!nsDir.exists() || !nsDir.isDirectory()) {
                return;
            }
        }
        addAll(nsDir, namespace, target);
    }

    private void addAll(File nsDir, String prefix, Map<String, String> target) {
        File[] children = nsDir.listFiles();
        for (File f : children) {
            if (f.isDirectory()) {
                String namespace = append(prefix, f.getName());
                addAll(f, namespace, target);
            } else if (f.getName().length() == 0 || f.getName().charAt(0) != '.') {
                String refName = append(prefix, f.getName());
                String refValue = readRef(f);
                target.put(refName, refValue);
            }
        }
    }

    @Override
    public Map<String, String> removeAll(String namespace) {
        final File file = toFile(namespace);
        if (file.exists() && file.isDirectory()) {
            deleteDir(file);
        }
        return null;
    }

    /**
     * @param directory
     */
    private void deleteDir(final File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new RuntimeException("Unable to list files of " + directory);
        }
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDir(f);
            } else if (!f.delete()) {
                throw new RuntimeException("Unable to delete file " + f.getAbsolutePath());
            }
        }
        if (!directory.delete()) {
            throw new RuntimeException("Unable to delete directory " + directory.getAbsolutePath());
        }
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.REF.configure(configDB, "file", "1.0");
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.REF.verify(configDB, "file", "1.0");
    }

    @Override
    public String toString() {
        Optional<URL> envHome = new ResolveGeogitDir(platform).call();
        return String.format("%s[geogit dir: %s]", getClass().getSimpleName(), envHome.orNull());
    }
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage.fs;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.geogit.api.Ref.append;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.AbstractRefDatabase;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Provides an implementation of a GeoGit ref database that utilizes the file system for the storage
 * of refs.
 */
public class FileRefDatabase extends AbstractRefDatabase {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final Platform platform;

    private final ConfigDatabase configDB;

    /**
     * Constructs a new {@code FileRefDatabase} with the given platform.
     * 
     * @param platform the platform to use
     */
    @Inject
    public FileRefDatabase(Platform platform, ConfigDatabase configDB) {
        this.platform = platform;
        this.configDB = configDB;
    }

    /**
     * Creates the reference database.
     */
    @Override
    public void create() {
        Optional<URL> envHome = new ResolveGeogitDir(platform).call();
        checkState(envHome.isPresent(), "Not inside a geogit directory");

        final URL envURL = envHome.get();
        if (!"file".equals(envURL.getProtocol())) {
            throw new UnsupportedOperationException(
                    "This References Database works only against file system repositories. "
                            + "Repository location: " + envURL.toExternalForm());
        }
        File repoDir;
        try {
            repoDir = new File(envURL.toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        File refs = new File(repoDir, "refs");
        if (!refs.exists() && !refs.mkdir()) {
            throw new IllegalStateException("Cannot create refs directory '"
                    + refs.getAbsolutePath() + "'");
        }
    }

    /**
     * Closes the reference database.
     */
    @Override
    public void close() {
        // nothing to close
    }

    /**
     * @param name the name of the ref (e.g. {@code "refs/remotes/origin"}, etc).
     * @return the ref, or {@code null} if it doesn't exist
     */
    @Override
    public String getRef(String name) {
        checkNotNull(name);
        File refFile = toFile(name);
        if (!refFile.exists() || refFile.isDirectory()) {
            return null;
        }
        String value = readRef(refFile);
        if (value == null) {
            return null;
        }
        try {
            ObjectId.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        return value;
    }

    /**
     * @param name the name of the symbolic ref (e.g. {@code "HEAD"}, etc).
     * @return the ref, or {@code null} if it doesn't exist
     */
    @Override
    public String getSymRef(String name) {
        checkNotNull(name);
        File refFile = toFile(name);
        if (!refFile.exists() || refFile.isDirectory()) {
            return null;
        }
        String value = readRef(refFile);
        if (value == null) {
            return null;
        }
        if (!value.startsWith("ref: ")) {
            throw new IllegalArgumentException(name + " is not a symbolic ref: '" + value + "'");
        }
        return value.substring("ref: ".length());
    }

    /**
     * @param refName the name of the ref
     * @param refValue the value of the ref
     * @return {@code null} if the ref didn't exist already, its old value otherwise
     */
    @Override
    public void putRef(String refName, String refValue) {
        checkNotNull(refName);
        checkNotNull(refValue);
        try {
            ObjectId.forString(refValue);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        store(refName, refValue);
    }

    /**
     * @param name the name of the symbolic ref
     * @param val the value of the symbolic ref
     * @return {@code null} if the ref didn't exist already, its old value otherwise
     */
    @Override
    public void putSymRef(String name, String val) {
        checkNotNull(name);
        checkNotNull(val);
        checkArgument(!name.equals(val), "Trying to store cyclic symbolic ref: %s", name);
        val = "ref: " + val;
        store(name, val);
    }

    /**
     * @param refName the name of the ref to remove (e.g. {@code "HEAD"},
     *        {@code "refs/remotes/origin"}, etc).
     * @return the value of the ref before removing it, or {@code null} if it didn't exist
     */
    @Override
    public String remove(String refName) {
        checkNotNull(refName);
        File refFile = toFile(refName);
        String oldRef;
        if (refFile.exists()) {
            oldRef = readRef(refFile);
            if (!refFile.delete()) {
                throw new RuntimeException("Unable to delete ref file '"
                        + refFile.getAbsolutePath() + "'");
            }
        } else {
            oldRef = null;
        }
        return oldRef;
    }

    /**
     * @param refPath
     * @return
     */
    private File toFile(String refPath) {
        Optional<URL> envHome = new ResolveGeogitDir(platform).call();

        String[] path = refPath.split("/");

        try {
            File file = new File(envHome.get().toURI());
            for (String subpath : path) {
                file = new File(file, subpath);
            }
            return file;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String readRef(final File refFile) {
        try {
            // make sure no other thread changes the ref as we read it
            synchronized (refFile.getCanonicalPath().intern()) {
                return Files.readFirstLine(refFile, CHARSET);
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * @param refName the full name of the ref (e.g.
     *        {@code refs/heads/master, HEAD, transaction/<tx id>/refs/orig/refs/heads/master, etc.}
     * @param refValue
     */
    private void store(String refName, String refValue) {
        final File refFile = toFile(refName);
        try {
            synchronized (refFile.getCanonicalPath().intern()) {
                Files.createParentDirs(refFile);
                checkState(refFile.exists() || refFile.createNewFile(),
                        "Unable to create file for ref %s", refFile);

                FileOutputStream fout = new FileOutputStream(refFile);
                try {
                    FileDescriptor fd = fout.getFD();
                    fout.write((refValue + "\n").getBytes(CHARSET));
                    fout.flush();
                    // force change to be persisted to disk
                    fd.sync();
                } finally {
                    fout.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw Throwables.propagate(e);
        }
    }

    /**
     * @return all known references under the "refs" namespace (i.e. not top level ones like HEAD,
     *         etc), key'ed by ref name
     */
    @Override
    public Map<String, String> getAll() {
        return getAll("refs");
    }

    /**
     * @return all references under the specified namespace
     */
    @Override
    public Map<String, String> getAll(String namespace) {
        File refsRoot;
        try {
            Optional<URL> envHome = new ResolveGeogitDir(platform).call();
            refsRoot = new File(envHome.get().toURI());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        if (namespace.endsWith("/")) {
            namespace = namespace.substring(0, namespace.length() - 1);
        }
        Map<String, String> refs = Maps.newTreeMap();
        findRefs(refsRoot, namespace, refs);
        return ImmutableMap.copyOf(refs);
    }

    private void findRefs(final File refsRoot, final String namespace,
            final Map<String, String> target) {
        String[] subdirs = namespace.split("/");
        File nsDir = refsRoot;
        for (String subdir : subdirs) {
            nsDir = new File(nsDir, subdir);
            if (!nsDir.exists() || !nsDir.isDirectory()) {
                return;
            }
        }
        addAll(nsDir, namespace, target);
    }

    private void addAll(File nsDir, String prefix, Map<String/* name */, String/* value */> target) {
        File[] children = nsDir.listFiles();
        for (File f : children) {
            final String fileName = f.getName();
            if (f.isDirectory()) {
                String namespace = append(prefix, fileName);
                addAll(f, namespace, target);
            } else if (fileName.length() == 0 || fileName.charAt(0) != '.') {
                String refName = append(prefix, fileName);
                String refValue = readRef(f);
                target.put(refName, refValue);
            }
        }
    }

    @Override
    public Map<String, String> removeAll(String namespace) {
        final File file = toFile(namespace);
        if (file.exists() && file.isDirectory()) {
            deleteDir(file);
        }
        return null;
    }

    /**
     * @param directory
     */
    private void deleteDir(final File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new RuntimeException("Unable to list files of " + directory);
        }
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDir(f);
            } else if (!f.delete()) {
                throw new RuntimeException("Unable to delete file " + f.getAbsolutePath());
            }
        }
        if (!directory.delete()) {
            throw new RuntimeException("Unable to delete directory " + directory.getAbsolutePath());
        }
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.REF.configure(configDB, "file", "1.0");
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.REF.verify(configDB, "file", "1.0");
    }

    @Override
    public String toString() {
        Optional<URL> envHome = new ResolveGeogitDir(platform).call();
        return String.format("%s[geogit dir: %s]", getClass().getSimpleName(), envHome.orNull());
    }
}
>>>>>>> .merge_file_4aNdNO
