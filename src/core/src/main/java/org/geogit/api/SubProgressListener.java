/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

public class SubProgressListener extends DefaultProgressListener {

    /** Initial starting value */
    float start;

    /** Amount of work we have been asked to perform */
    float amount;

    /** Scale between subprogress and delegate */
    float scale;

    ProgressListener parentProgressListener;

    /**
     * Create a sub progress monitor, used to delegate work to a separate process.
     * 
     * @param progress parent progress to notify as we get work done
     * @param amount amount of progress represented
     */
    public SubProgressListener(ProgressListener progress, float amount) {
        super();
        parentProgressListener = progress;
        this.start = progress.getProgress();
        this.amount = (amount > 0.0f) ? amount : 0.0f;
        float max = parentProgressListener.getMaxProgress();
        this.scale = this.amount / max;
    }

    public void started() {
        super.progress = 0.0f;
    }

    public void complete() {
        parentProgressListener.setProgress(start + amount);
        progress = getMaxProgress();
    }

    public float getProgress() {
        return progress;
    }

    public void progress(float progress) {
        this.progress = progress;
        parentProgressListener.setProgress(start + (scale * progress));
    }
}