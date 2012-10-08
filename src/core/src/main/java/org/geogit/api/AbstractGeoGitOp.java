/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.geotools.util.NullProgressListener;
import org.geotools.util.SubProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.google.inject.Inject;

public abstract class AbstractGeoGitOp<T> implements Callable<T> {

    private static final ProgressListener NULL_PROGRESS_LISTENER = new NullProgressListener();

    protected final Logger LOGGER;

    private ProgressListener progressListener = NULL_PROGRESS_LISTENER;

    private CommandLocator commandLocator;

    public AbstractGeoGitOp() {
        LOGGER = Logging.getLogger(getClass());
    }

    public <C extends AbstractGeoGitOp<?>> C command(Class<C> commandClass) {
        return commandLocator.command(commandClass);
    }

    @Inject
    public void setCommandLocator(CommandLocator locator) {
        this.commandLocator = locator;
    }

    public AbstractGeoGitOp<T> setProgressListener(final ProgressListener listener) {
        this.progressListener = listener == null ? NULL_PROGRESS_LISTENER : listener;
        return this;
    }

    protected ProgressListener getProgressListener() {
        return progressListener;
    }

    protected ProgressListener subProgress(float amount) {
        return new SubProgressListener(getProgressListener(), amount);
    }

    /**
     * @see java.util.concurrent.Callable#call()
     */
    public abstract T call();

}
