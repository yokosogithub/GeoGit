/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.Map;
import java.util.Map.Entry;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.DescribeOp;
import org.geotools.data.DataStore;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 *
 */
@Parameters(commandNames = "describe", commandDescription = "Describe a PostGIS table")
public class PGDescribe extends AbstractPGCommand implements CLICommand {

    @Parameter(names = { "--table", "-t" }, description = "Table to describe.", required = true)
    public String table = "";

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }
        if (table.isEmpty()) {
            cli.getConsole().println("No table supplied");
            return;
        }

        DataStore dataStore = null;
        try {
            dataStore = getDataStore();
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
            cli.getConsole().flush();
            return;
        }

        try {
            if (dataStore instanceof JDBCDataStore) {
                Connection con = null;
                try {
                    con = ((JDBCDataStore) dataStore).getDataSource().getConnection();
                } catch (Exception e) {
                    throw new ConnectException();
                }

                ((JDBCDataStore) dataStore).closeSafe(con);
            }

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
                cli.getConsole().println("Could not find the specified table.");
            }
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}
