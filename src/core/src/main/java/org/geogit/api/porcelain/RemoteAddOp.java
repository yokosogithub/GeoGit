/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.porcelain.RemoteException.StatusCode;
import org.geogit.storage.ConfigDatabase;

import com.google.inject.Inject;

/**
 * Adds a remote to the local config database.
 * 
 * @see ConfigDatabase
 * @see Remote
 */
public class RemoteAddOp extends AbstractGeoGitOp<Remote> {

    private String name;

    private String url;

    private String branch;

    private String username;

    private String password;

    private boolean mapped = false;

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

        List<String> allRemotes = config.getAllSubsections("remote");
        if (allRemotes.contains(name)) {
            throw new RemoteException(StatusCode.REMOTE_ALREADY_EXISTS);
        }

        String configSection = "remote." + name;
        String fetch = "+" + Ref.HEADS_PREFIX + branch + ":" + Ref.REMOTES_PREFIX + name + "/"
                + branch;

        config.put(configSection + ".url", url);
        config.put(configSection + ".fetch", fetch);
        if (mapped) {
            config.put(configSection + ".mapped", "true");
            config.put(configSection + ".mappedBranch", branch);
        }
        if (username != null) {
            config.put(configSection + ".username", username);
        }
        if (password != null) {
            password = Remote.encryptPassword(password);
            config.put(configSection + ".password", password);
        }

        return new Remote(name, url, url, fetch, mapped, branch, username, password);
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
     * @param url the URL of the remote
     * @return {@code this}
     */
    public RemoteAddOp setURL(String url) {
        this.url = url;
        return this;
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
     * @param username user name for the repository
     * @return {@code this}
     */
    public RemoteAddOp setUserName(String username) {
        this.username = username;
        return this;
    }

    /**
     * @param password password for the repository
     * @return {@code this}
     */
    public RemoteAddOp setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * @param mapped whether or not this is a mapped remote
     * @return {@code this}
     */
    public RemoteAddOp setMapped(boolean mapped) {
        this.mapped = mapped;
        return this;
    }

}
