/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.geogit.api.Remote;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.RemoteListOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 *
 */
@Parameters(commandNames = "list", commandDescription = "List all remotes for the current repository")
public class RemoteList extends AbstractCommand implements CLICommand {

    @Parameter(names = { "-v", "--verbose" }, description = "Be a little more verbose and show remote url after name.")
    boolean verbose = false;

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        try {
            final Optional<List<Remote>> remoteList = cli.getGeogit().command(RemoteListOp.class)
                    .call();

            if (remoteList.isPresent()) {
                for (Remote remote : remoteList.get()) {
                    if (verbose) {
                        cli.getConsole().println(
                                remote.getName() + " " + remote.getFetchURL() + " (fetch)");
                        cli.getConsole().println(
                                remote.getName() + " " + remote.getPushURL() + " (push)");
                    } else {
                        cli.getConsole().println(remote.getName());
                    }
                }
            }
        } catch (ConfigException e) {
            cli.getConsole().println("Could not access the config database.");
        }
    }

}
