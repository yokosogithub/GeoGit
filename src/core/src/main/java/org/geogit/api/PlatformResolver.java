/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import org.geogit.api.porcelain.CommitStateResolver;

public class PlatformResolver implements CommitStateResolver {

    /**
     * @see org.geogit.api.porcelain.CommitStateResolver.data.versioning.AuthenticationResolver#getAuthor()
     */
    @Override
    public String getAuthor() {
        String userName = System.getProperty("user.name", "anonymous");
        return userName;
    }

    @Override
    public String getCommitMessage() {
        return null;
    }

    @Override
    public String getCommitter() {
        return getAuthor();
    }

    @Override
    public long getCommitTimeMillis() {
        return System.currentTimeMillis();
    }

}
