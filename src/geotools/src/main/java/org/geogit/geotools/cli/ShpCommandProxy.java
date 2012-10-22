/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.geotools.porcelain.ShpImport;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for shp specific commands.
 * 
 * @see ShpImport
 */
@Parameters(commandNames = "shp", commandDescription = "GeoGit/Shapefile integration utilities")
public class ShpCommandProxy implements CLICommandExtension {

    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit shp");
        commander.addCommand("import", new ShpImport());
        return commander;
    }
}
