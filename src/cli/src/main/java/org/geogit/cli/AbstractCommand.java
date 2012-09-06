/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * A template command; provides out of the box support for the --help argument so far.
 * 
 */
public abstract class AbstractCommand implements CLICommand {

    @Parameter(names = "--help", help = true, hidden = true)
    private boolean help;

    /**
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        if (help) {
            JCommander jc = new JCommander(this);
            String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
            jc.setProgramName("geogit " + commandName);
            jc.usage();
            return;
        }

        runInternal(cli);
    }

    /**
     * Subclasses shall implement to do the real work, will not be called if the command was invoked
     * with {@code --help}
     */
    protected abstract void runInternal(GeogitCLI cli) throws Exception;
}
