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
 * Update remote refs along with associated objects.
 */
public class PushOp extends AbstractGeoGitOp<Void> {

    private boolean all;

    private boolean rebase;

    private String repository;

    private List<String> refSpecs = new ArrayList<String>();

    private final Platform platform;

    /**
     * Constructs a new {@code PushOp} with the given parameters.
     * 
     * @param platform the current platform
     */
    @Inject
    public PushOp(final Platform platform) {
        this.platform = platform;
    }

    /**
     * @param all if {@code true}, push all refs under refs/heads/
     * @return {@code this}
     */
    public PushOp setAll(final boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @return whether or not the op is configured to push all refs
     */
    public boolean getAll() {
        return all;
    }

    /**
     * @param refSpec the refspec of a remote branch
     * @return {@code this}
     */
    public PushOp addRefSpec(final String refSpec) {
        refSpecs.add(refSpec);
        return this;
    }

    /**
     * @return the list of refspecs to push
     */
    public List<String> getRefSpecs() {
        return refSpecs;
    }

    /**
     * @param repository the repository to push to
     * @return {@code this}
     */
    public PushOp setRepository(final String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * @return the repository to push to
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Executes the push operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public Void call() {

        return null;
    }
}
