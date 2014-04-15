/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.geotools.cli.porcelain.SQLServerDescribe;
import org.geogit.geotools.cli.porcelain.SQLServerExport;
import org.geogit.geotools.cli.porcelain.SQLServerImport;
import org.geogit.geotools.cli.porcelain.SQLServerList;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for SQL Server specific commands.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit sqlserver <command> <args>...}
 * </ul>
 * 
 * @see SQLServerImport
 * @see SQLServerList
 * @see SQLServerDescribe
 * @see SQLServerExport
 */
@Parameters(commandNames = "sqlserver", commandDescription = "GeoGit/SQL Server integration utilities")
public class SQLServerCommandProxy implements CLICommandExtension {

    /**
     * @return the JCommander parser for this extension
     * @see JCommander
     */
    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit sqlserver");
        commander.addCommand("import", new SQLServerImport());
        commander.addCommand("list", new SQLServerList());
        commander.addCommand("describe", new SQLServerDescribe());
        commander.addCommand("export", new SQLServerExport());

        return commander;
    }
}
