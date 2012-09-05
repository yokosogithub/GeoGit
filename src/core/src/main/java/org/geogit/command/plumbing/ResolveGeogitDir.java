/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.command.plumbing;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;

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
public class ResolveGeogitDir extends AbstractGeoGitOp<URL> {

    private Platform platform;

    @Inject
    public ResolveGeogitDir(Platform platform) {
        super(null);
        this.platform = platform;
    }

    /**
     * @return
     * @throws Exception
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    @Override
    public URL call() {
        File pwd = platform.pwd();
        URL lookup;
        try {
            lookup = lookupGeogitDirectory(pwd);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return lookup;
    }

    /**
     * @param pwd
     * @return
     * @throws IOException
     * @throws Exception
     */
    private URL lookupGeogitDirectory(File file) throws IOException {
        if (file == null) {
            return null;
        }
        if (file.isDirectory()) {
            if (file.getName().equals(".geogit")) {
                return file.toURI().toURL();
            }
            File[] contents = file.listFiles();
            for (File dir : contents) {
                if (dir.isDirectory() && dir.getName().equals(".geogit")) {
                    return lookupGeogitDirectory(dir);
                }
            }
        }
        return lookupGeogitDirectory(file.getParentFile());
    }

}
