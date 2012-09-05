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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.net.URL;

import org.geogit.api.GeoGIT;
import org.junit.Test;

/**
 *
 */
public class ResolveGeogitDirTest {

    @Test
    public void test() throws Exception {

        File workingDir = new File("target", "mockWorkingDir");
        File fakeRepo = new File(workingDir, ".geogit");
        fakeRepo.mkdirs();

        URL resolvedRepoDir = new GeoGIT(workingDir).command(ResolveGeogitDir.class).call();
        assertEquals(fakeRepo.toURI().toURL(), resolvedRepoDir);

        workingDir = new File(new File(workingDir, "subdir1"), "subdir2");
        workingDir.mkdirs();

        resolvedRepoDir = new GeoGIT(workingDir).command(ResolveGeogitDir.class).call();
        assertEquals(fakeRepo.toURI().toURL(), resolvedRepoDir);

        assertNull(new GeoGIT(new File("target")).command(ResolveGeogitDir.class).call());

    }

}
