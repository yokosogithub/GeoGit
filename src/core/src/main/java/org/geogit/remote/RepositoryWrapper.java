/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import org.geogit.api.ObjectId;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Provides an interface to make basic queries to a repository.
 */
interface RepositoryWrapper {

    /**
     * Determines if the provided object exists in the repository.
     * 
     * @param objectId the object to look for
     * @return true if the object existed, false otherwise
     */
    public boolean objectExists(ObjectId objectId);

    /**
     * Gets the parents of the specified commit from the repository.
     * 
     * @param commit the id of the commit whose parents to retrieve
     * @return a list of parent ids for the commit
     */
    public ImmutableList<ObjectId> getParents(ObjectId commitId);

    /**
     * Gets the depth of the given commit.
     * 
     * @param commitId the commit id
     * @return the depth, or 0 if the commit was not found
     */
    public int getDepth(ObjectId commitId);

    /**
     * Gets the depth of the repository.
     * 
     * @return the depth of the repository, or {@link Optional#absent()} if the repository is not
     *         shallow
     */
    public Optional<Integer> getRepoDepth();
}
