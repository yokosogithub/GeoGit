/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.remote;

import java.io.IOException;

import org.geogit.api.Ref;
import org.geogit.repository.Repository;

import com.google.common.collect.ImmutableSet;

/**
 *
 */
public interface IRemoteRepo {

    /**
     * 
     */
    public void open() throws IOException;

    public void close() throws IOException;

    /**
     * @param getHeads whether to return refs in the {@code refs/heads} namespace
     * @param getTags whether to return refs in the {@code refs/tags} namespace
     * @return
     */
    public ImmutableSet<Ref> listRefs(boolean getHeads, boolean getTags);

    public void fetchNewData(Repository localRepository, Ref ref);
}
