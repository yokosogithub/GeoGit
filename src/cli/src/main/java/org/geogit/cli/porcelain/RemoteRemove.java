/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.List;

import org.geogit.api.porcelain.RemoteException;
import org.geogit.api.porcelain.RemoteRemoveOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ObjectDatabaseReadOnly;
import org.geogit.cli.annotation.StagingDatabaseReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Remove the remote named <name>. All remote-tracking branches and configuration settings for the
 * remote are removed.
 * 
 * <p>
 * CLI proxy for {@link RemoteRemoveOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit rm <name>}
 * </ul>
 * 
 * @see RemoteRemoveOp
 */
@ObjectDatabaseReadOnly
@StagingDatabaseReadOnly
@Parameters(commandNames = "rm", commandDescription = "Remove a remote from the repository")
public class RemoteRemove extends AbstractCommand implements CLICommand {

    @Parameter(description = "<name>")
    private List<String> params;

    /**
     * Executes the remote remove command.
     */
    @Override
    public void runInternal(GeogitCLI cli) {
        if (params == null || params.size() != 1) {
            printUsage();
            throw new CommandFailedException();
        }

        try {
            cli.getGeogit().command(RemoteRemoveOp.class).setName(params.get(0)).call();
        } catch (RemoteException e) {
            switch (e.statusCode) {
            case REMOTE_NOT_FOUND:
                throw new CommandFailedException("Could not find a remote called '" + params.get(0)
                        + "'.", e);
            default:
                throw new CommandFailedException(e.getMessage(), e);
            }
        }
    }

}
