/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;

/**
 * Resolves the location of the {@code .geogit} repository directory relative to the
 * {@link Platform#pwd() current directory}.
 * <p>
 * The location can be a either the current directory, a parent of it, or {@code null} if no
 * {@code .geogit} directory is found.
 * 
 */
public class ResolveGeogitDir extends AbstractGeoGitOp<Optional<URL>> {

    private Platform platform;

    /**
     * Constructs a new instance of {@code ResolveGeogitDir} with the specified platform.
     * 
     * @param platform the platform to use
     */
    @Inject
    public ResolveGeogitDir(Platform platform) {
        this.platform = platform;
    }

    public static Optional<URL> lookup(final File directory) {
        try {
            return Optional.fromNullable(lookupGeogitDirectory(directory));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * @return the location of the {@code .geogit} repository environment directory or {@code null}
     *         if not inside a working directory
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    @Override
    public Optional<URL> call() {
        File pwd = platform.pwd();
        Optional<URL> repoLocation = ResolveGeogitDir.lookup(pwd);
        return repoLocation;
    }

    /**
     * @param file the directory to search
     * @return the location of the {@code .geogit} repository environment directory or {@code null}
     *         if not inside a working directory
     */
    private static URL lookupGeogitDirectory(@Nullable File file) throws IOException {
        if (file == null) {
            return null;
        }
        if (file.isDirectory()) {
            if (file.getName().equals(".geogit")) {
                return file.toURI().toURL();
            }
            File[] contents = file.listFiles();
            Preconditions.checkNotNull(contents,
                    "Either '%s' is not a directory or an I/O error ocurred listing its contents",
                    file.getAbsolutePath());
            for (File dir : contents) {
                if (dir.isDirectory() && dir.getName().equals(".geogit")) {
                    return lookupGeogitDirectory(dir);
                }
            }
        }
        return lookupGeogitDirectory(file.getParentFile());
    }

}
