/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import org.geogit.api.ObjectId;
import org.geogit.repository.Repository;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Provides an interface to make basic queries to a local repository.
 */
class LocalRepositoryWrapper implements RepositoryWrapper {

    private Repository localRepository;

    /**
     * Constructs a new {@code LocalRepositoryWrapper} using the provided repository.
     * 
     * @param repository the local repository
     */
    public LocalRepositoryWrapper(Repository repository) {
        this.localRepository = repository;
    }

    /**
     * Determines if the provided object exists in the repository.
     * 
     * @param objectId the object to look for
     * @return true if the object existed, false otherwise
     */
    @Override
    public boolean objectExists(ObjectId objectId) {
        return objectId.isNull() || localRepository.getObjectDatabase().exists(objectId);
    }

    /**
     * Gets the parents of the specified commit from the repository.
     * 
     * @param commit the id of the commit whose parents to retrieve
     * @return a list of parent ids for the commit
     */
    @Override
    public ImmutableList<ObjectId> getParents(ObjectId commitId) {
        return localRepository.getGraphDatabase().getParents(commitId);
    }

    /**
     * Gets the depth of the given commit.
     * 
     * @param commitId the commit id
     * @return the depth, or 0 if the commit was not found
     */
    @Override
    public int getDepth(ObjectId commitId) {
        return localRepository.getGraphDatabase().getDepth(commitId);
    }

    /**
     * Gets the depth of the repository.
     * 
     * @return the depth of the repository, or {@link Optional#absent()} if the repository is not
     *         shallow
     */
    @Override
    public Optional<Integer> getRepoDepth() {
        return localRepository.getDepth();
    }

}
