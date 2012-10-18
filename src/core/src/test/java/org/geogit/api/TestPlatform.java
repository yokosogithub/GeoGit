/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.File;

public class TestPlatform extends DefaultPlatform implements Platform {

    private File workingDirectory;

    private File userHomeDirectory;

    public TestPlatform(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public File pwd() {
        return workingDirectory;
    }

    public void setUserHome(final File userHomeDirectory) {
        this.userHomeDirectory = userHomeDirectory;
    }

    @Override
    public File getUserHome() {
        return userHomeDirectory;
    }
}
