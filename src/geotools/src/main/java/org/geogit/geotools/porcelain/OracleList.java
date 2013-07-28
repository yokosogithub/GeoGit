/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.net.ConnectException;
import java.util.List;

import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ListOp;
import org.geotools.data.DataStore;

import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Lists tables from an Oracle database.
 * 
 * Oracle CLI proxy for {@link ListOp}
 * 
 * @see ListOp
 */
@Parameters(commandNames = "list", commandDescription = "List available feature types in a database")
public class OracleList extends AbstractOracleCommand implements CLICommand {

    /**
     * Executes the list command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractOracleCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        DataStore dataStore = null;
        try {
            dataStore = getDataStore();
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
            throw new CommandFailedException();
        }

        try {
            cli.getConsole().println("Fetching feature types...");

            Optional<List<String>> features = cli.getGeogit().command(ListOp.class)
                    .setDataStore(dataStore).call();

            if (features.isPresent()) {
                for (String featureType : features.get()) {
                    cli.getConsole().println(" - " + featureType);
                }
            } else {
                cli.getConsole().println("No features types were found in the specified database.");
                throw new CommandFailedException();
            }
        } catch (GeoToolsOpException e) {
            cli.getConsole().println("Unable to get feature types from the database.");
            throw new CommandFailedException();
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }

}
