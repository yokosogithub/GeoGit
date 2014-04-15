/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.TimeZone;

import org.geogit.api.plumbing.ResolveGeogitDir;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * Standard platform for GeoGit.
 */
public class DefaultPlatform implements Platform {

    protected File workingDir;

    /**
     * @return the working directory
     */
    @Override
    public File pwd() {
        if (workingDir != null) {
            return workingDir;
        }
        return new File(".").getAbsoluteFile().getParentFile();
    }

    /**
     * @param workingDir the working directory to use
     * @throws IllegalArgumentException if {@code workingDir} does not exist or is not a directory
     */
    @Override
    public void setWorkingDir(File workingDir) {
        checkArgument(workingDir == null || workingDir.isDirectory(),
                "file does not exist or is not a directory: " + workingDir);
        this.workingDir = workingDir;
    }

    /**
     * @see Platform#whoami()
     */
    @Override
    public String whoami() {
        return System.getProperty("user.name", "nobody");
    }

    /**
     * @return the current time in milliseconds
     * @see org.geogit.api.Platform#currentTimeMillis()
     */
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * @return the user home directory
     */
    @Override
    public File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    @Override
    public int timeZoneOffset(long timeStamp) {
        return TimeZone.getDefault().getOffset(timeStamp);
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    @Override
    public File getTempDir() {
        Optional<URL> url = new ResolveGeogitDir(this).call();
        final File tmpDir;
        if (url.isPresent()) {
            try {
                URI uri = url.get().toURI();
                tmpDir = new File(new File(uri), "tmp");
                Preconditions.checkState(tmpDir.exists() || tmpDir.mkdir(),
                        "unable to create directory %s", tmpDir.getAbsolutePath());
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        } else {
            String systmplocation = System.getProperty("java.io.tmpdir");
            tmpDir = new File(systmplocation);
        }
        return tmpDir;
    }

    @Override
    public int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

}
