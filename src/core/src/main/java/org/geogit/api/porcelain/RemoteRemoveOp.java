package org.geogit.api.porcelain;

import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Remote;
import org.geogit.api.porcelain.RemoteException.StatusCode;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Removes a remote from the local config database.
 * 
 * @author jgarrett
 * @see ConfigDatabase
 */
public class RemoteRemoveOp extends AbstractGeoGitOp<Remote> {

    private String name;

    final private ConfigDatabase config;

    /**
     * Constructs a new {@code RemoteRemoveOp} with the given config database.
     * 
     * @param config where the remote is stored
     */
    @Inject
    public RemoteRemoveOp(ConfigDatabase config) {
        this.config = config;
    }

    /**
     * Executes the remote-remove operation.
     * 
     * @return the {@link Remote} that was removed, or {@link Optional#absent()} if the remote
     *         didn't exist.
     */
    @Override
    public Remote call() {
        if (name == null || name.isEmpty()) {
            throw new RemoteException(StatusCode.MISSING_NAME);
        }
        List<String> allRemotes = config.getAllSubsections("remote");
        if (!allRemotes.contains(name)) {
            throw new RemoteException(StatusCode.REMOTE_NOT_FOUND);
        }

        Remote remote = null;
        String remoteSection = "remote." + name;
        Optional<String> remoteFetchURL = config.get(remoteSection + ".url");
        Optional<String> remoteFetch = config.get(remoteSection + ".fetch");
        Optional<String> remotePushURL = Optional.absent();
        if (remoteFetchURL.isPresent() && remoteFetch.isPresent()) {
            remotePushURL = config.get(remoteSection + ".pushurl");
        }

        remote = new Remote(name, remoteFetchURL.or(""), remotePushURL.or(remoteFetchURL.or("")),
                remoteFetch.or(""));

        config.removeSection(remoteSection);

        return remote;
    }

    /**
     * @param name the name of the remote to remove
     * @return {@code this}
     */
    public RemoteRemoveOp setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the name of the remote to remove
     */
    public String getName() {
        return name;
    }

}
