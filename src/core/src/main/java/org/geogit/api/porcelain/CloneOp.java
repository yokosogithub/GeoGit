/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Clones a remote repository to a given directory.
 * 
 */
public class CloneOp extends AbstractGeoGitOp<Void> {

    private Optional<String> branch;

    private String repositoryURL;

    private String directory;

    private final Platform platform;

    /**
     * Constructs a new {@code CloneOp} with the given parameters.
     * 
     * @param platform the current platform
     */
    @Inject
    public CloneOp(final Platform platform) {
        this.platform = platform;
    }

    /**
     * @param repositoryURL the URL of the repository to clone
     * @return {@code this}
     */
    public CloneOp setRepositoryURL(final String repositoryURL) {
        this.repositoryURL = repositoryURL;
        return this;
    }

    /**
     * @return the URL of the repository to clone.
     */
    public String getRepositoryURL() {
        return repositoryURL;
    }

    /**
     * @param directory the directory to clone the remote repository to
     * @return {@code this}
     */
    public CloneOp setDirectory(final String directory) {
        this.directory = directory;
        return this;
    }

    /**
     * @return the directory to clone the remote repository to.
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @param branch the branch to checkout when the clone is complete
     * @return {@code this}
     */
    public CloneOp setBranch(@Nullable String branch) {
        this.branch = Optional.fromNullable(branch);
        return this;
    }

    /**
     * @return the branch to checkout when the clone is complete.
     */
    public Optional<String> getBranch() {
        return branch;
    }

    /**
     * Executes the clone operation.
     * 
     * @return {@code null}
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    public Void call() {

        return null;
    }
}
