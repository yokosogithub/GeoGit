/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
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
     * @see System#nanoTime()
     */
    public long nanoTime();

    /**
     * @return the user's home directory
     */
    public File getUserHome();

    /**
     * Returns the offset of the platform's time zone from UTC at the specified timeStamp.
     * 
     * @param timeStamp the date represented in milliseconds since January 1, 1970 00:00:00 GMT
     * @return the amount of time in milliseconds to add to UTC to get local time.
     */
    public int timeZoneOffset(long timeStamp);

    /**
     * @return the maximum number of processors available to the virtual machine; never smaller than
     *         one, as in {@link Runtime#availableProcessors()}
     */
    public int availableProcessors();

    /**
     * @return a directory suitable to be used to store temporary files, may be inside the
     *         repository's directory structure or the one provided by the system.
     */
    public File getTempDir();
}
