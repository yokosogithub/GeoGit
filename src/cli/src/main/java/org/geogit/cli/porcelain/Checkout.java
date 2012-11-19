/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;

/**
 *
 */
@Parameters(commandNames = "checkout", commandDescription = "Checkout a branch or paths to the working tree")
public class Checkout extends AbstractCommand implements CLICommand {

    @Parameter(arity = 1, description = "<branch|commit>")
    private List<String> branchOrStartPoint = Lists.newArrayList();

    @Parameter(names = { "--force", "-f" }, description = "When switching branches, proceed even if the index or the "
            + "working tree differs from HEAD. This is used to throw away local changes.")
    private boolean force = false;

    @Override
    public void runInternal(GeogitCLI cli) {
        final GeoGIT geogit = cli.getGeogit();
        checkState(geogit != null, "not in a geogit repository.");
        checkArgument(branchOrStartPoint.size() == 1,
                "<branch> not specified or too many arguments");

        // final ConsoleReader console = cli.getConsole();
        String branchOrCommit = branchOrStartPoint.get(0);

        geogit.command(CheckoutOp.class).setForce(force).setSource(branchOrCommit).call();
    }

}
