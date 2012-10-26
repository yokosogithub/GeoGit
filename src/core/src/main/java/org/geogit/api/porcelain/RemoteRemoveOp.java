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
     * @param config where the remote is stored
     */
    @Inject
    public RemoteRemoveOp(ConfigDatabase config) {
        this.config = config;
    }

    /**
     * @return the remote that was removed, or Optional.absent if the remote didn't exist.
     */
    @Override
    public Remote call() {
        if (name == null || name.isEmpty()) {
            throw new RemoteException(StatusCode.MISSING_NAME);
        }
        Optional<List<String>> allRemotes = config.getAllSubsections("remote");
        if (!allRemotes.isPresent() || !allRemotes.get().contains(name)) {
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

    public RemoteRemoveOp setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

}
