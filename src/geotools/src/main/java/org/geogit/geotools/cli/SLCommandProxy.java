/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.geotools.cli.porcelain.SLDescribe;
import org.geogit.geotools.cli.porcelain.SLExport;
import org.geogit.geotools.cli.porcelain.SLImport;
import org.geogit.geotools.cli.porcelain.SLList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for SpatiaLite specific commands.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit sl <command> <args>...}
 * </ul>
 * 
 * @see SLImport
 * @see SLList
 * @see SLDescribe
 * @see SLExport
 */
@Parameters(commandNames = "sl", commandDescription = "GeoGit/SpatiaLite integration utilities")
public class SLCommandProxy implements CLICommandExtension {

    /**
     * @return the JCommander parser for this extension
     * @see JCommander
     */
    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit sl");
        commander.addCommand("import", new SLImport());
        commander.addCommand("list", new SLList());
        commander.addCommand("describe", new SLDescribe());
        commander.addCommand("export", new SLExport());
        return commander;
    }
}
