/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli;

import java.io.File;

/**
 *
 */
public class DefaultPlatform implements Platform {

    @Override
    public File pwd() {
        return new File(".");
    }

    @Override
    public String whoami() {
        return System.getProperty("user.name", "nobody");
    }

    /**
     * @return
     * @see org.geogit.cli.Platform#currentTimeMillis()
     */
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

}
