package org.geogit.remote;

import java.io.IOException;
import java.net.URL;

import org.geogit.api.Ref;
import org.geogit.repository.Repository;

import com.google.common.collect.ImmutableSet;

/**
 * An implementation of a remote repository that exists on a remote machine.
 * 
 * @see IRemoteRepo
 */
public class HTTPRemoteRepo implements IRemoteRepo {

    private URL repositoryURL;

    /**
     * Constructs a new {@code HTTPRemoteRepo} with the given parameters.
     * 
     * @param repositoryURL the url of the remote repository
     */
    public HTTPRemoteRepo(URL repositoryURL) {
        this.repositoryURL = repositoryURL;
    }

    /**
     * Opens the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void open() throws IOException {

    }

    /**
     * Closes the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException {

    }

    /**
     * @return the remote's HEAD {@link Ref}.
     */
    @Override
    public Ref headRef() {
        return null;
    }

    /**
     * List the remote's {@link Ref refs}.
     * 
     * @param getHeads whether to return refs in the {@code refs/heads} namespace
     * @param getTags whether to return refs in the {@code refs/tags} namespace
     * @return an immutable set of refs from the remote
     */
    @Override
    public ImmutableSet<Ref> listRefs(final boolean getHeads, final boolean getTags) {
        return null;
    }

    /**
     * Fetch all new objects from the specified {@link Ref} from the remote.
     * 
     * @param localRepository the repository to add new objects to
     * @param ref the remote ref that points to new commit data
     */
    @Override
    public void fetchNewData(Repository localRepository, Ref ref) {
    }

    /**
     * Push all new objects from the specified {@link Ref} to the remote.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     */
    @Override
    public void pushNewData(Repository localRepository, Ref ref) {
    }

    @Override
    public void pushNewData(Repository localRepository, Ref ref, String refspec) {

    }

    @Override
    public void deleteRef(Repository localRepository, String refspec) {

    }
}
