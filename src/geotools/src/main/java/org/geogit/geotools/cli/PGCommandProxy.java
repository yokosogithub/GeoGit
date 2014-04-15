/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.geotools.cli.porcelain.PGDescribe;
import org.geogit.geotools.cli.porcelain.PGExport;
import org.geogit.geotools.cli.porcelain.PGImport;
import org.geogit.geotools.cli.porcelain.PGList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for PostGIS specific commands.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit pg <command> <args>...}
 * </ul>
 * 
 * @see PGImport
 * @see PGList
 * @see PGDescribe
 * @see PGExport
 */
@Parameters(commandNames = "pg", commandDescription = "GeoGit/PostGIS integration utilities")
public class PGCommandProxy implements CLICommandExtension {

    /**
     * @return the JCommander parser for this extension
     * @see JCommander
     */
    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit pg");
        commander.addCommand("import", new PGImport());
        commander.addCommand("list", new PGList());
        commander.addCommand("describe", new PGDescribe());
        commander.addCommand("export", new PGExport());

        return commander;
    }
}
