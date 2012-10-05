/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.test.functional;

import java.io.File;

import org.geogit.api.DefaultPlatform;
import org.geogit.api.Platform;

public class TestPlatform extends DefaultPlatform implements Platform {

    private File workingDirectory;

    public TestPlatform(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public File pwd() {
        return workingDirectory;
    }

    @Override
    public File getUserHome() {
        return new File(workingDirectory, "userhome");
    }
}
