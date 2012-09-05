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
package org.geogit.api;

import java.io.File;
import java.net.URL;

import org.geogit.command.plumbing.ResolveGeogitDir;
import org.geogit.repository.Repository;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * 
 */
public class InitOp extends AbstractGeoGitOp<Repository> {

    private Platform platform;

    private Injector injector;

    @Inject
    public InitOp(Platform platform, Injector injector) {
        super(null);
        this.platform = platform;
        this.injector = injector;
    }

    /**
     */
    @Override
    public Repository call() throws Exception {
        final File pwd = platform.pwd();
        final URL repoUrl = new ResolveGeogitDir(platform).call();

        if (repoUrl != null) {
            if (!repoUrl.equals(new File(pwd, ".geogit").toURI().toURL())) {
                throw new IllegalArgumentException("Already inside a geogit directory: "
                        + repoUrl.toExternalForm());
            }
        }

        File envHome = new File(pwd, ".geogit");
        envHome.mkdirs();
        if (!envHome.exists()) {
            throw new RuntimeException("Unable to create geogit environment at '"
                    + envHome.getAbsolutePath() + "'");
        }

        Preconditions.checkState(envHome.toURI().toURL()
                .equals(new ResolveGeogitDir(platform).call()));

        Repository repository = injector.getInstance(Repository.class);
        repository.create();

        return repository;
    }

}
