/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.porcelain;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;
import org.geogit.geotools.plumbing.DescribeOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geotools.data.DataStore;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;


@ReadOnly
@Parameters(commandNames = "describe", commandDescription = "Describe a SQL Server table")
public class SQLServerDescribe extends AbstractSQLServerCommand implements CLICommand {

    /**
     * Table to describe.
     */
    @Parameter(names = { "--table", "-t" }, description = "Table to describe.", required = true)
    public String table = "";

    /**
     * Executes the describe command using the provided options.
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        DataStore dataStore = getDataStore();

        try {
            cli.getConsole().println("Fetching table...");

            Optional<Map<String, String>> propertyMap = cli.getGeogit().command(DescribeOp.class)
                    .setTable(table).setDataStore(dataStore).call();

            if (propertyMap.isPresent()) {
                cli.getConsole().println("Table : " + table);
                cli.getConsole().println("----------------------------------------");
                for (Entry<String, String> entry : propertyMap.get().entrySet()) {
                    cli.getConsole().println("\tProperty  : " + entry.getKey());
                    cli.getConsole().println("\tType      : " + entry.getValue());
                    cli.getConsole().println("----------------------------------------");
                }
            } else {
                throw new CommandFailedException("Could not find the specified table.");
            }
        } catch (GeoToolsOpException e) {
            switch (e.statusCode) {
            case TABLE_NOT_DEFINED:
                throw new CommandFailedException("No table supplied.", e);
            case UNABLE_TO_GET_FEATURES:
                throw new CommandFailedException("Unable to read the feature source.", e);
            case UNABLE_TO_GET_NAMES:
                throw new CommandFailedException("Unable to read feature types.", e);
            default:
                throw new CommandFailedException("Exception: " + e.statusCode.name(), e);
            }

        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}