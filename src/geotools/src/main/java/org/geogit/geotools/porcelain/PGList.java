/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.List;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ListOp;
import org.geotools.data.DataStore;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 *
 */
@Parameters(commandNames = "list", commandDescription = "List available feature types in a database")
public class PGList extends AbstractPGCommand implements CLICommand {

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
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

            cli.getConsole().println("Fetching feature types...");

            Optional<List<String>> features = cli.getGeogit().command(ListOp.class)
                    .setDataStore(dataStore).call();
            ;

            if (features.isPresent()) {
                for (String featureType : features.get()) {
                    cli.getConsole().println(" - " + featureType);
                }
            } else {
                cli.getConsole().println("No features types were found in the specified database.");
            }
        } catch (GeoToolsOpException e) {
            cli.getConsole().println("Unable to get feature types from the database.");
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}
