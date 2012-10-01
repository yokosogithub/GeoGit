/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.net.URL;

import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.InitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.di.GeogitModule;
import org.geogit.repository.Repository;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A facade to Geo-GIT operations.
 * <p>
 * Represents the checkout of user's working tree and repository and provides the operations to work
 * on them.
 * </p>
 * 
 * @author groldan
 */
public class GeoGIT {

    private final Injector injector;

    private Repository repository;

    public GeoGIT() {
        injector = Guice.createInjector(new GeogitModule());
    }

    public GeoGIT(File workingDir) {
        this();
        injector.getInstance(Platform.class).setWorkingDir(workingDir);
    }

    public GeoGIT(final Injector injector) {
        this(injector, null);
    }

    public GeoGIT(final Injector injector, final File workingDir) {
        this.injector = injector;
        injector.getInstance(Platform.class).setWorkingDir(workingDir);
    }

    public void close() {
        if (repository != null) {
            repository.close();
            repository = null;
        }
    }

    /**
     * @param commandClass
     */
    public <T extends AbstractGeoGitOp<?>> T command(Class<T> commandClass) {
        return injector.getInstance(commandClass);
    }

    /**
     * Obtains the repository for the current directory or creates a new one and returns it if no
     * repository can be found on the current directory.
     * 
     * @return the existing or newly created repository, never {@code null}
     * @throws RuntimeException if the repository cannot be created at the current directory
     * @see InitOp
     */
    public Repository getOrCreateRepository() {
        if (getRepository() == null) {
            try {
                repository = command(InitOp.class).call();
                checkState(repository != null,
                        "Repository shouldn't be null as we checked it didn't exist before calling init");
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return repository;
    }

    /**
     * @return the configured repository or {@code null} if no repository is found on the current
     *         directory
     */
    public synchronized Repository getRepository() {
        if (repository != null) {
            return repository;
        }

        final URL repoLocation = command(ResolveGeogitDir.class).call();
        final boolean repoFound = null != repoLocation;
        if (repoFound) {
            try {
                repository = injector.getInstance(Repository.class);
                repository.create();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return repository;
    }

    /**
     * Add a transaction record to the index
     */
    public AddOp add() {
        return command(AddOp.class);
    }

    /**
     * Record changes to the repository
     * 
     * @return commit id
     */
    public CommitOp commit() {
        CommitOp command = command(CommitOp.class);
        return command;
    }

    /**
     * Check out a branch to the working tree
     */
    public CheckoutOp checkout() {
        return command(CheckoutOp.class);
    }

    /**
     * Show changes between commits, commit and working tree, etc
     */
    public DiffOp diff() {
        return command(DiffOp.class);
    }

    /**
     * Show commit logs
     */
    public LogOp log() {
        return command(LogOp.class);
    }

    /**
     * @return
     */
    public Platform getPlatform() {
        return injector.getInstance(Platform.class);
    }

}
