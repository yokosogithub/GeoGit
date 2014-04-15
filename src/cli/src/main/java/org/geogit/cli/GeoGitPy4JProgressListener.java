/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli;

/**
 * An interface for progress listener for the Py4j entry point.
 * 
 * Implementation should be done on the Python side
 */
public interface GeoGitPy4JProgressListener {

    public void setProgress(float i);

    public void setProgressText(String s);

}