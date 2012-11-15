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
 * Incorporates changes from a remote repository into the current branch.
 */
public class PullOp extends AbstractGeoGitOp<Void> {

    private boolean all;

    private boolean rebase;

    private String repository;

    private List<String> refSpecs = new ArrayList<String>();

    private final Platform platform;

    /**
     * Constructs a new {@code PullOp} with the given parameters.
     * 
     * @param platform the current platform
     */
    @Inject
    public PullOp(final Platform platform) {
        this.platform = platform;
    }

    /**
     * @param all if {@code true}, pull from all remotes.
     * @return {@code this}
     */
    public PullOp setAll(final boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @return whether or not the op is configured to pull from all remotes
     */
    public boolean getAll() {
        return all;
    }

    /**
     * @param rebase if {@code true}, perform a rebase on the remote branch instead of a merge
     * @return {@code this}
     */
    public PullOp setRebase(final boolean rebase) {
        this.rebase = rebase;
        return this;
    }

    /**
     * @return whether or not to rebase instead of merge the remote branch
     */
    public boolean getRebase() {
        return rebase;
    }

    /**
     * @param refSpec the refspec of a remote branch
     * @return {@code this}
     */
    public PullOp addRefSpec(final String refSpec) {
        refSpecs.add(refSpec);
        return this;
    }

    /**
     * @return the list of refspecs to pull
     */
    public List<String> getRefSpecs() {
        return refSpecs;
    }

    /**
     * @param repository the repository to pull from
     * @return {@code this}
     */
    public PullOp setRepository(final String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * @return the repository to pull from.
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Executes the pull operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public Void call() {

        return null;
    }
}
