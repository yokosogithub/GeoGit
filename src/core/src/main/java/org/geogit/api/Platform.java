/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api;

import java.io.File;

/**
 *
 */
public interface Platform {

    public File pwd();

    /**
     * Sets the working directory, or {@code null} to default to the JVM working directory
     */
    public void setWorkingDir(File workingDir);

    public String whoami();

    /**
     * @return
     */
    public long currentTimeMillis();

}
