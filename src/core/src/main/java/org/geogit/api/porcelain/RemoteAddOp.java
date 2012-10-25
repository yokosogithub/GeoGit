package org.geogit.api.porcelain;

import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Remote;
import org.geogit.api.porcelain.RemoteException.StatusCode;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * Adds a remote to the local config database.
 * 
 * @author jgarrett
 * @see ConfigDatabase
 */
public class RemoteAddOp extends AbstractGeoGitOp<Remote> {

    private String name;

    private String url;

    private String branch;

    final private ConfigDatabase config;

    /**
     * @param config where to store the remote
     */
    @Inject
    public RemoteAddOp(ConfigDatabase config) {
        this.config = config;
    }

    /**
     * @return Optional.absent();
     */
    @Override
    public Remote call() {
        if (name == null || name.isEmpty()) {
            throw new RemoteException(StatusCode.MISSING_NAME);
        }
        if (url == null || url.isEmpty()) {
            throw new RemoteException(StatusCode.MISSING_URL);
        }
        if (branch == null || branch.isEmpty()) {
            branch = "*";
        }

        Optional<List<String>> allRemotes = config.getAllSubsections("remote");
        if (allRemotes.isPresent()) {
            if (allRemotes.get().contains(name)) {
                throw new RemoteException(StatusCode.REMOTE_ALREADY_EXISTS);
            }
        }

        String configSection = "remote." + name;
        String fetch = "+refs/heads/" + branch + ":refs/remotes/" + name + "/" + branch;

        config.put(configSection + ".url", url);
        config.put(configSection + ".fetch", fetch);

        return new Remote(name, url, url, fetch);
    }

    public RemoteAddOp setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public RemoteAddOp setURL(String url) {
        this.url = url;
        return this;
    }

    public String getURL() {
        return url;
    }

    public RemoteAddOp setBranch(String branch) {
        this.branch = branch;
        return this;
    }

    public String getBranch() {
        return branch;
    }

}
