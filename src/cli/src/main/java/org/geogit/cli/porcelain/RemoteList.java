/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.IOException;

import org.geogit.api.Remote;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.RemoteListOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;

/**
 * Shows a list of existing remotes.
 * <p>
 * With the {@code -v} option, be a little more descriptive and show the remote URL after the name.
 * <p>
 * CLI proxy for {@link RemoteListOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit remote list [-v]}
 * </ul>
 * 
 * @see RemoteListOp
 */
@ReadOnly
@Parameters(commandNames = "list", commandDescription = "List all remotes for the current repository")
public class RemoteList extends AbstractCommand implements CLICommand {

    @Parameter(names = { "-v", "--verbose" }, description = "Be a little more verbose and show remote url after name.")
    boolean verbose = false;

    /**
     * Executes the remote list command.
     */
    @Override
    public void runInternal(GeogitCLI cli) throws IOException {

        final ImmutableList<Remote> remoteList;
        try {
            remoteList = cli.getGeogit().command(RemoteListOp.class).call();
        } catch (ConfigException e) {
            throw new CommandFailedException("Could not access the config database.", e);
        }

        for (Remote remote : remoteList) {
            if (verbose) {
                cli.getConsole()
                        .println(remote.getName() + " " + remote.getFetchURL() + " (fetch)");
                cli.getConsole().println(remote.getName() + " " + remote.getPushURL() + " (push)");
            } else {
                cli.getConsole().println(remote.getName());
            }
        }

    }

}
