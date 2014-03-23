/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

public interface ProgressListener {

    /**
     * Returns the description of the current task being run
     * 
     * @return the description of the current task being run
     */
    String getDescription();

    /**
     * Sets the description of the current task being run
     * 
     * @param description
     */
    void setDescription(String description);

    /**
     * Notifies this listener that the operation begins.
     */
    void started();

    /**
     * Sets the current progress value
     * 
     * @param progress the progress value
     * 
     */
    void setProgress(float progress);

    /**
     * Sets the current max progress
     * 
     * @param maxProgress the maximum value of the progress value
     */
    void setMaxProgress(float maxProgress);

    /**
     * Returns the maximum progress value
     * 
     * @return the maximum progress value
     */
    public float getMaxProgress();

    /**
     * Returns the current progress
     * 
     * @return the current progress
     */
    float getProgress();

    /**
     * Notifies this listener that the operation has finished.
     */
    void complete();

    /**
     * Releases any resources used by this listener.
     */
    void dispose();

    /**
     * Returns {@code true} if this job is cancelled.
     * 
     * @return {@code true} if this job is cancelled.
     */
    boolean isCanceled();

    /**
     * Cancels the task
     */
    void cancel();

}
