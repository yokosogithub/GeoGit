/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * A default progress listener to be used for extending it.
 * 
 * It is also a functional listener that does not show progress, so it can actually be used as a
 * silet progress listener
 * 
 */
public class DefaultProgressListener implements ProgressListener {
    /**
     * Description of the current action.
     */
    protected String description;

    /**
     * Current progress value
     */
    protected float progress;

    /**
     * {@code true} if the action is canceled.
     */
    protected boolean canceled = false;

    /**
     * {@code true} if the action has already been completed.
     */
    protected boolean completed = false;

    /**
     * The maximum expected value of the progress.
     * 
     * By default, it has a value of 100, so it assumes that the progress value is a percent value
     */
    protected float maxProgress;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void started() {
        // do nothing
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public float getProgress() {
        return progress;
    }

    public void complete() {
        this.completed = true;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void dispose() {
        // do nothing
    }

    public void cancel() {
        this.canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setMaxProgress(float maxProgress) {
        this.maxProgress = maxProgress;

    }

    public float getMaxProgress() {
        return this.maxProgress;
    }

}
