/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.remote;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;

import org.geogit.api.Remote;
import org.geogit.repository.Repository;
import org.geogit.storage.DeduplicationService;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Injector;

/**
 * Provides utilities for creating interfaces to remote repositories.
 */
public class RemoteUtils {

    /**
     * Constructs an interface to allow access to a remote repository.
     * 
     * @param injector a Guice injector for the new repository
     * @param remoteConfig the remote to connect to
     * @param localRepository the local repository
     * @return an {@link Optional} of the interface to the remote repository, or
     *         {@link Optional#absent()} if a connection to the remote could not be established.
     */
    public static Optional<IRemoteRepo> newRemote(Injector injector, Remote remoteConfig,
            Repository localRepository, DeduplicationService deduplicationService) {

        try {
            URI fetchURI = URI.create(remoteConfig.getFetchURL());
            String protocol = fetchURI.getScheme();

            IRemoteRepo remoteRepo = null;
            if (protocol == null || protocol.equals("file")) {
                String filepath = new URL(remoteConfig.getFetchURL()).getFile();
                if (remoteConfig.getMapped()) {
                    remoteRepo = new LocalMappedRemoteRepo(injector, new File(filepath),
                            localRepository);
                } else {
                    remoteRepo = new LocalRemoteRepo(injector, new File(filepath), localRepository);
                }
            } else if (protocol.equals("http")) {
                final String username = remoteConfig.getUserName();
                final String password = remoteConfig.getPassword();
                if (username != null && password != null) {
                    Authenticator.setDefault(new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, Remote.decryptPassword(
                                    password).toCharArray());
                        }
                    });
                } else {
                    Authenticator.setDefault(null);
                }
                if (remoteConfig.getMapped()) {
                    remoteRepo = new HttpMappedRemoteRepo(fetchURI.toURL(), localRepository);
                } else {
                    remoteRepo = new HttpRemoteRepo(fetchURI.toURL(), localRepository,
                            deduplicationService);
                }
            } else {
                throw new UnsupportedOperationException(
                        "Only file and http remotes are currently supported.");
            }
            return Optional.fromNullable(remoteRepo);
        } catch (Exception e) {
            // Invalid fetch URL
            Throwables.propagate(e);
        }

        return Optional.absent();
    }
}
