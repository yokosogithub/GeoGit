/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.File;
import java.util.List;

import org.geogit.api.merge.MergeOp;
import org.geogit.command.plumbing.PlumbingCommands;
import org.geogit.repository.Repository;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.PropertyName;
import org.opengis.util.ProgressListener;

import com.google.common.base.Preconditions;
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
@SuppressWarnings("rawtypes")
public class GeoGIT {

    private Repository repository;

    public static final CommitStateResolver DEFAULT_COMMIT_RESOLVER = new PlatformResolver();

    private static CommitStateResolver commitStateResolver = DEFAULT_COMMIT_RESOLVER;

    private Injector injector;

    public GeoGIT(File workingDir) {
        this();
        injector.getInstance(Platform.class).setWorkingDir(workingDir);
    }

    public GeoGIT() {
        injector = Guice.createInjector(new GeogitModule(), new PlumbingCommands(),
                new PorcelainCommnds());
    }

    public GeoGIT(final Injector injector) {
        this(injector, null);
    }

    public GeoGIT(final Injector injector, final File workingDir) {
        this.injector = injector;
        injector.getInstance(Platform.class).setWorkingDir(workingDir);
    }

    public GeoGIT(final Repository repository) {
        Preconditions.checkNotNull(repository, "repository can't be null");
        this.repository = repository;
    }

    /**
     * @param commandClass
     */
    public <T extends AbstractGeoGitOp> T command(Class<T> commandClass) {
        return injector.getInstance(commandClass);
    }

    public static CommitStateResolver getCommitStateResolver() {
        return commitStateResolver;
    }

    public static void setCommitStateResolver(CommitStateResolver resolver) {
        commitStateResolver = resolver == null ? new PlatformResolver() : resolver;
    }

    public Repository getRepository() {
        if (repository != null) {
            return repository;
        }
        if (injector != null) {

            try {
                repository = command(InitOp.class).call();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            return repository;
        }
        throw new IllegalStateException();
    }

    public static CloneOp clone(final String url) {
        return null;// new CloneOp(url);
    }

    /**
     * Add a transaction record to the index
     */
    public AddOp add() {
        return new AddOp(repository);
    }

    /**
     * Remove files from the working tree and from the index
     * 
     */
    public String rm(final String user, final Name typeName,
            final FeatureCollection affectedFeatures, final ProgressListener progressListener) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return
     */
    public String update(final String user, final Name typeName,
            final List<PropertyName> changedProperties, final FeatureCollection affectedFeatures,
            final ProgressListener progressListener) {

        throw new UnsupportedOperationException();
    }

    /**
     * Record changes to the repository
     * 
     * @return commit id
     */
    public CommitOp commit() {
        return new CommitOp(repository, commitStateResolver);
    }

    /**
     * List, create or delete branches
     */
    public BranchCreateOp branchCreate() {
        return new BranchCreateOp(repository);
    }

    public BranchDeleteOp branchDelete() {
        return new BranchDeleteOp(repository);
    }

    /**
     * Check out a branch to the working tree
     */
    public CheckoutOp checkout() {
        return new CheckoutOp(repository);
    }

    /**
     * Show changes between commits, commit and working tree, etc
     */
    public DiffOp diff() {
        return new DiffOp(repository);
    }

    /**
     * Create an empty working tree or reinitialize an existing one
     */
    public void init() {

    }

    /**
     * Show commit logs
     */
    public LogOp log() {
        return new LogOp(repository);
    }

    /**
     * Join two or more development histories together
     */
    public MergeOp merge() {
        return new MergeOp(repository);
    }

    /**
     * Forward-port local commits to the updated upstream head
     */
    public RebaseOp rebase() {
        return new RebaseOp(repository);
    }

    /**
     * Reset current HEAD to the specified state
     */
    public void reset() {

    }

    /**
     * Show various types of objects by their unique id
     * 
     * @return
     */
    public ShowOp show() {
        return new ShowOp(repository);
    }

    /**
     * Show the working tree status
     */
    public void status() {

    }

    /**
     * Create, list, delete or verify a tag object
     */
    public void tag() {

    }

}
