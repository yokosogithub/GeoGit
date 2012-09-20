/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage.fs;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.storage.RefDatabase;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 *
 */
public class FileRefDatabase implements RefDatabase {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private Platform platform;

    @Inject
    public FileRefDatabase(Platform platform) {
        this.platform = platform;
    }

    @Override
    public void create() {
        URL envHome = new ResolveGeogitDir(platform).call();
        if (envHome == null) {
            throw new IllegalStateException("Not inside a geogit directory");
        }
        if (!"file".equals(envHome.getProtocol())) {
            throw new UnsupportedOperationException(
                    "This References Database works only against file system repositories. "
                            + "Repository location: " + envHome.toExternalForm());
        }
        File repoDir;
        try {
            repoDir = new File(envHome.toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        File refs = new File(repoDir, "refs");
        if (!refs.exists() && !refs.mkdir()) {
            throw new IllegalStateException("Cannot create refs directory '"
                    + refs.getAbsolutePath() + "'");
        }
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public String getRef(String name) {
        checkNotNull(name);
        File refFile = toFile(name);
        if (!refFile.exists()) {
            return null;
        }
        String value = readRef(refFile);
        try {
            ObjectId.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        return value;
    }

    @Override
    public String getSymRef(String name) {
        checkNotNull(name);
        File refFile = toFile(name);
        if (!refFile.exists()) {
            return null;
        }
        String value = readRef(refFile);
        if (!value.startsWith("ref: ")) {
            throw new IllegalArgumentException(name + " is not a symbolic ref: '" + value + "'");
        }
        return value.substring("ref: ".length());
    }

    @Override
    public String putRef(String refName, String refValue) {
        checkNotNull(refName);
        checkNotNull(refValue);
        try {
            ObjectId.forString(refValue);
        } catch (IllegalArgumentException e) {
            throw e;
        }

        String oldRef = getRef(refName);

        store(refName, refValue);

        return oldRef;
    }

    @Override
    public String putSymRef(String name, String val) {
        checkNotNull(name);
        checkNotNull(val);

        String oldRef = getSymRef(name);

        val = "ref: " + val;
        store(name, val);

        return oldRef;
    }

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
        URL envHome = new ResolveGeogitDir(platform).call();

        String[] path = refPath.split("/");

        try {
            File file = new File(envHome.toURI());
            for (String subpath : path) {
                file = new File(file, subpath);
            }
            return file;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String readRef(File refFile) {
        try {
            return Files.readFirstLine(refFile, CHARSET);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void store(String refName, String refValue) {
        File refFile = toFile(refName);

        try {
            Files.createParentDirs(refFile);
            Files.write(refValue + "\n", refFile, CHARSET);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Map<String, String> getAll() {
        File refsRoot;
        try {
            URL envHome = new ResolveGeogitDir(platform).call();
            refsRoot = new File(new File(envHome.toURI()), "refs");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        Map<String, String> refs = Maps.newTreeMap();
        findRefs(refsRoot, "refs/", refs);
        return ImmutableMap.copyOf(refs);
    }

    private void findRefs(File refsDir, String prefix, Map<String, String> target) {

        File[] children = refsDir.listFiles();
        for (File f : children) {
            if (f.isDirectory()) {
                findRefs(f, prefix + f.getName() + "/", target);
            } else {
                String refName = prefix + f.getName();
                String refValue = readRef(f);
                target.put(refName, refValue);
            }
        }
    }

}
