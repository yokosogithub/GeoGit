/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.remote;

import java.io.File;
import java.net.URI;

import org.geogit.api.Remote;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Injector;

/**
 *
 */
public class RemoteUtils {

    /**
     * @param remoteConfig
     * @return
     */
    public static Optional<IRemoteRepo> newRemote(Injector injector, Remote remoteConfig) {

        try {
            URI fetchURI = URI.create(remoteConfig.getFetchURL());
            String protocol = fetchURI.getScheme();

            IRemoteRepo remoteRepo = null;
            if (protocol == null || protocol.equals("file")) {
                remoteRepo = new LocalRemoteRepo(injector, new File(remoteConfig.getFetchURL()));
            }
            return Optional.fromNullable(remoteRepo);
        } catch (Exception e) {
            // Invalid fetch URL
            Throwables.propagate(e);
        }

        return Optional.absent();
    }

}
