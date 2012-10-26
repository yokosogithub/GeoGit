package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Remote;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Return a list of all of the remotes from the local config database.
 * 
 * @author jgarrett
 * @see ConfigDatabase
 */
public class RemoteListOp extends AbstractGeoGitOp<Optional<List<Remote>>> {

    final private ConfigDatabase config;

    /**
     * @param config where to find the remotes
     */
    @Inject
    public RemoteListOp(ConfigDatabase config) {
        this.config = config;
    }

    /**
     * @return Optional<List<Remote>> of all remotes found in the config database.
     */
    @Override
    public Optional<List<Remote>> call() {
        Optional<List<String>> remotes = config.getAllSubsections("remote");
        if (remotes.isPresent()) {
            List<Remote> allRemotes = new ArrayList<Remote>();
            for (String remoteName : remotes.get()) {
                String remoteSection = "remote." + remoteName;
                Optional<String> remoteFetchURL = config.get(remoteSection + ".url");
                Optional<String> remoteFetch = config.get(remoteSection + ".fetch");
                if (remoteFetchURL.isPresent() && remoteFetch.isPresent()) {
                    Optional<String> remotePushURL = config.get(remoteSection + ".pushurl");
                    allRemotes.add(new Remote(remoteName, remoteFetchURL.get(), remotePushURL
                            .or(remoteFetchURL.get()), remoteFetch.get()));
                }

            }
            if (!allRemotes.isEmpty()) {
                return Optional.of(allRemotes);
            }

        }
        return Optional.absent();
    }
}
