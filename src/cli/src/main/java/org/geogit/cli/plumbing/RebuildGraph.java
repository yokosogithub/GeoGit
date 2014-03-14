/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.plumbing;

import java.io.IOException;

import jline.console.ConsoleReader;

import org.geogit.api.ObjectId;
import org.geogit.api.plumbing.RebuildGraphOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;

/**
 * Rebuilds the graph database and prints a list of commits that were incomplete or missing.
 * 
 * @see RebuildGraphOp
 */
@ReadOnly
@Parameters(commandNames = "rebuild-graph", commandDescription = "Rebuilds the graph database.")
public class RebuildGraph extends AbstractCommand implements CLICommand {

    @Parameter(names = "--quiet", description = "Print only a summary of the fixed entries.")
    private boolean quiet = false;

    @Override
    public void runInternal(GeogitCLI cli) throws IOException {
        ImmutableList<ObjectId> updatedObjects = cli.getGeogit().command(RebuildGraphOp.class)
                .call();

        final ConsoleReader console = cli.getConsole();
        if (updatedObjects.size() > 0) {
            if (quiet) {
                console.println(updatedObjects.size() + " graph elements (commits) were fixed.");
            } else {
                console.println("The following graph elements (commits) were incomplete or missing and have been fixed:");
                for (ObjectId object : updatedObjects) {
                    console.println(object.toString());
                }
            }
        } else {
            console.println("No missing or incomplete graph elements (commits) were found.");
        }
    }
}
