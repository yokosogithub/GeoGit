/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.geotools.porcelain.PGDescribe;
import org.geogit.geotools.porcelain.PGImport;
import org.geogit.geotools.porcelain.PGList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for pg specific commands.
 * 
 * @see PGImport
 */
@Parameters(commandNames = "pg", commandDescription = "GeoGit/PostGIS integration utilities")
public class PGCommandProxy implements CLICommandExtension {

    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit pg");
        commander.addCommand("import", new PGImport());
        commander.addCommand("list", new PGList());
        commander.addCommand("describe", new PGDescribe());
        return commander;
    }
}
