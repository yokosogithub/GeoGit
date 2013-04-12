/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli;

/**
 * Marker interface to aid in looking up CLI commands
 */
public interface CLICommand {

    /**
     * @param cli
     * 
     */
    void run(GeogitCLI cli) throws Exception;

}
