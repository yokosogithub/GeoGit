/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.test.functional;

import java.io.File;

import org.geogit.api.DefaultPlatform;
import org.geogit.api.Platform;

public class PGTestPlatform extends DefaultPlatform implements Platform {

    private File workingDirectory;

    public PGTestPlatform(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public File pwd() {
        return workingDirectory;
    }

    @Override
    public File getUserHome() {
        File userhome = new File(workingDirectory, "userhome");
        userhome.mkdir();
        return userhome;
    }
}
