/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;

import com.google.inject.Inject;

/**
 * Fetches named heads or tags from one or more other repositories, along with the objects necessary
 * to complete them.
 */
public class FetchOp extends AbstractGeoGitOp<Void> {

    private boolean all;

    private boolean prune;

    private List<String> repositories = new ArrayList<String>();

    private final Platform platform;

    /**
     * Constructs a new {@code FetchOp} with the given parameters.
     * 
     * @param platform the current platform
     */
    @Inject
    public FetchOp(final Platform platform) {
        this.platform = platform;
    }

    /**
     * @param all if {@code true}, fetch from all remotes.
     * @return {@code this}
     */
    public FetchOp setAll(final boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @return whether or not the op is configured to fetch from all remotes
     */
    public boolean getAll() {
        return all;
    }

    /**
     * @param prune if {@code true}, remote tracking branches that no longer exist will be removed
     *        locally.
     * @return {@code this}
     */
    public FetchOp setPrune(final boolean prune) {
        this.prune = prune;
        return this;
    }

    /**
     * @return whether or not the op is configured to remove remote tracking branches that no longer
     *         exist.
     */
    public boolean getPrune() {
        return prune;
    }

    /**
     * @param repository the name or URL of a remote repository to fetch from
     * @return {@code this}
     */
    public FetchOp addRepository(final String repository) {
        repositories.add(repository);
        return this;
    }

    /**
     * @return the list of remote repositories that will be fetched from
     */
    public List<String> getRepositories() {
        return repositories;
    }

    /**
     * Executes the fetch operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public Void call() {

        return null;
    }
}
