/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.pg.cli;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import org.geotools.data.DataStore;
import org.geotools.jdbc.JDBCDataStore;

import org.opengis.feature.type.Name;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@Parameters(commandNames = "list", commandDescription = "List available feature types in a database")
public class PGList extends AbstractPGCommand implements CLICommand {

    @ParametersDelegate
    public PGListArgs args = new PGListArgs();

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        DataStore dataStore = null;
        try {

            dataStore = getDataStore();
            if (dataStore instanceof JDBCDataStore) {
                Connection con = null;
                try {
                    con = ((JDBCDataStore) dataStore).getDataSource().getConnection();
                } catch (Exception e) {
                    throw new ConnectException();
                }

                ((JDBCDataStore) dataStore).closeSafe(con);
            }

            doList(cli, dataStore);
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
        } finally {
            if (dataStore != null) {
                dataStore.dispose();
            }
            cli.getConsole().flush();
        }
    }

    private void doList(GeogitCLI cli, DataStore dataStore) throws Exception {
        final ConsoleReader console = cli.getConsole();

        console.println("Fetching Feature Types...");

        List<Name> typeNames = dataStore.getNames();
        for (Name typeName : typeNames) {
            console.println(" - " + typeName);
        }
    }
}
