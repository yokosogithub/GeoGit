/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.map.cli;

import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.map.internal.OSMUnmapOp;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Updates the raw OSM data of the repository (stored in the "node" and "way" trees), with the data
 * in a tree that represents a mapped version of that raw data
 * 
 * @see OSMUnmapOp
 */
@Parameters(commandNames = "unmap", commandDescription = "Updates the raw OSM data, unmapping the mapped OSM data in a given tree in the working tree")
public class OSMUnmap extends AbstractCommand implements CLICommand {

    @Parameter(description = "<path>")
    public List<String> args;

    private GeoGIT geogit;

    /**
     * Executes the map command using the provided options.
     * 
     * @param cli
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        if (args == null || args.isEmpty() || args.size() != 1) {
            printUsage();
            throw new CommandFailedException();
        }

        String path = args.get(0);

        geogit = cli.getGeogit();

        ObjectId oldTreeId = geogit.getRepository().getWorkingTree().getTree().getId();

        ObjectId newTreeId = geogit.command(OSMUnmapOp.class).setPath(path).call().getId();

        ConsoleReader console = cli.getConsole();
        if (newTreeId.equals(oldTreeId)) {
            console.println("No differences were found after unmapping.\n"
                    + "No changes have been made to the working tree");
        } else {
            // print something?
        }
    }

}
