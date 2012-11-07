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
 * @see Remote
 */
public class RemoteAddOp extends AbstractGeoGitOp<Remote> {

    private String name;

    private String url;

    private String branch;

    final private ConfigDatabase config;

    /**
     * Constructs a new {@code RemoteAddOp} with the given config database.
     * 
     * @param config where to store the remote
     */
    @Inject
    public RemoteAddOp(ConfigDatabase config) {
        this.config = config;
    }

    /**
     * Executes the remote-add operation.
     * 
     * @return the {@link Remote} that was added.
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

    /**
     * @param name the name of the remote
     * @return {@code this}
     */
    public RemoteAddOp setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the name of the remote
     */
    public String getName() {
        return name;
    }

    /**
     * @param url the URL of the remote
     * @return {@code this}
     */
    public RemoteAddOp setURL(String url) {
        this.url = url;
        return this;
    }

    /**
     * @return the URL of the remote
     */
    public String getURL() {
        return url;
    }

    /**
     * @param branch a specific branch to track
     * @return {@code this}
     */
    public RemoteAddOp setBranch(String branch) {
        this.branch = branch;
        return this;
    }

    /**
     * @return the branch being tracked, {@code null} if no specific branch is set
     */
    public String getBranch() {
        return branch;
    }

}
