/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;

/**
 *
 */
public class DefaultPlatform implements Platform {

    private File workingDir;

    @Override
    public File pwd() {
        if (workingDir != null) {
            return workingDir;
        }
        return new File(".").getAbsoluteFile().getParentFile();
    }

    public void setWorkingDir(File workingDir) {
        checkArgument(workingDir == null || workingDir.isDirectory(),
                "file does not exist or is not a directory: " + workingDir);
        this.workingDir = workingDir;
    }

    @Override
    public String whoami() {
        return System.getProperty("user.name", "nobody");
    }

    /**
     * @return
     * @see org.geogit.api.Platform#currentTimeMillis()
     */
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

}
