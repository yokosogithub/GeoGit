/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli;

import org.springframework.context.ApplicationContext;

/**
 * Marker interface to aid in looking up CLI commands on the {@link ApplicationContext}
 */
public interface CLICommand {

    /**
     * @param cli
     * 
     */
    void run(GeogitCLI cli) throws Exception;

}
