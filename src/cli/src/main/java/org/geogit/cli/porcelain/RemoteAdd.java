/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.porcelain.RemoteAddOp;
import org.geogit.api.porcelain.RemoteException;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 */
@Parameters(commandNames = "remote add", commandDescription = "Add a remote for the repository")
public class RemoteAdd extends AbstractCommand implements CLICommand {

    @Parameter(names = { "-t", "--track" }, description = "branch to track")
    private String branch = "*";

    @Parameter(description = "<name> <url>")
    private List<String> params = new ArrayList<String>();

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        if (params == null || params.size() != 2) {
            printUsage();
            return;
        }

        try {
            cli.getGeogit().command(RemoteAddOp.class).setName(params.get(0)).setURL(params.get(1))
                    .setBranch(branch).call();
        } catch (RemoteException e) {
            switch (e.statusCode) {
            case REMOTE_ALREADY_EXISTS:
                cli.getConsole().println(
                        "Could not add, a remote called '" + params.get(0) + "' already exists.");
                break;
            default:
                break;
            }
        }

    }

}
