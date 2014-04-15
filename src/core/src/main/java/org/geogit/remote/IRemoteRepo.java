/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.remote;

import java.io.IOException;

import org.geogit.api.Ref;
import org.geogit.api.porcelain.SynchronizationException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 * Provides an interface for interacting with remote repositories.
 */
public interface IRemoteRepo {

    /**
     * Opens the remote repository.
     * 
     * @throws IOException
     */
    public void open() throws IOException;

    /**
     * Closes the remote repository.
     * 
     * @throws IOException
     */
    public void close() throws IOException;

    /**
     * List the remote's {@link Ref refs}.
     * 
     * @param getHeads whether to return refs in the {@code refs/heads} namespace
     * @param getTags whether to return refs in the {@code refs/tags} namespace
     * @return an immutable set of refs from the remote
     */
    public ImmutableSet<Ref> listRefs(boolean getHeads, boolean getTags);

    /**
     * @return the remote's HEAD {@link Ref}.
     */
    public Ref headRef();

    /**
     * Fetch all new objects from the specified {@link Ref} from the remote.
     * 
     * @param ref the remote ref that points to new commit data
     * @param fetchLimit the maximum depth to fetch
     */
    public void fetchNewData(Ref ref, Optional<Integer> fetchLimit);

    /**
     * Push all new objects from the specified {@link Ref} to the remote.
     * 
     * @param ref the local ref that points to new commit data
     */
    public void pushNewData(Ref ref) throws SynchronizationException;

    /**
     * Push all new objects from the specified {@link Ref} to the given refspec.
     * 
     * @param ref the local ref that points to new commit data
     * @param refspec the refspec to push to
     */
    public void pushNewData(Ref ref, String refspec) throws SynchronizationException;

    /**
     * Delete the given refspec from the remote repository.
     * 
     * @param refspec the refspec to delete
     */
    public void deleteRef(String refspec);

    /**
     * @return the depth of the repository, or {@link Optional#absent} if the repository is not
     *         shallow
     */
    public Optional<Integer> getDepth();

}
