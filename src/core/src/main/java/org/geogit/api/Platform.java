/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api;

import java.io.File;

/**
 * Interface for a GeoGit platform.
 */
public interface Platform {

    /**
     * @return the working directory
     */
    public File pwd();

    /**
     * Sets the working directory, or {@code null} to default to the JVM working directory
     */
    public void setWorkingDir(File workingDir);

    /**
     * @return who I am
     */
    public String whoami();

    /**
     * @return the current time in milliseconds
     */
    public long currentTimeMillis();

    /**
     * @return the user's home directory
     */
    public File getUserHome();
}
