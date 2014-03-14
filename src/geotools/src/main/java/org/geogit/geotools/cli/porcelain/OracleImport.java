/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.porcelain;

import java.io.IOException;

import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ObjectDatabaseReadOnly;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geotools.data.DataStore;
import org.opengis.util.ProgressListener;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Imports one or more tables from an Oracle database.
 * 
 * Oracle CLI proxy for {@link ImportOp}
 * 
 * @see ImportOp
 */
@ObjectDatabaseReadOnly
@Parameters(commandNames = "import", commandDescription = "Import Oracle database")
public class OracleImport extends AbstractOracleCommand implements CLICommand {

    /**
     * If this is set, only this table will be imported.
     */
    @Parameter(names = { "--table", "-t" }, description = "Table to import.")
    public String table = "";

    /**
     * If this is set, all tables will be imported.
     */
    @Parameter(names = "--all", description = "Import all tables.")
    public boolean all = false;

    /**
     * do not overwrite or delete features
     */
    @Parameter(names = { "--add" }, description = "Do not replace or delete features in the destination path")
    boolean add;

    /**
     * Set the path default feature type to the the feature type of imported features, and modify
     * existing features to match it
     */
    @Parameter(names = { "--alter" }, description = "Set the path default feature type to the the feature type of imported features, and modify existing features to match it")
    boolean alter;

    /**
     * Destination path to add features to. Only allowed when importing a single table
     */
    @Parameter(names = { "-d", "--dest" }, description = "Path to import to")
    String destTable;

    /**
     * Executes the import command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractOracleCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {

        DataStore dataStore = getDataStore();

        try {
            cli.getConsole().println("Importing from database " + commonArgs.database);

            ProgressListener progressListener = cli.getProgressListener();
            cli.getGeogit().command(ImportOp.class).setAll(all).setTable(table).setAlter(alter)
                    .setDestinationPath(destTable).setOverwrite(!add).setDataStore(dataStore)
                    .setProgressListener(progressListener).call();

            cli.getConsole().println("Import successful.");

        } catch (GeoToolsOpException e) {
            switch (e.statusCode) {
            case TABLE_NOT_DEFINED:
                cli.getConsole().println(
                        "No tables specified for import. Specify --all or --table <table>.");
                throw new CommandFailedException();
            case ALL_AND_TABLE_DEFINED:
                cli.getConsole().println("Specify --all or --table <table>, both cannot be set.");
                throw new CommandFailedException();
            case NO_FEATURES_FOUND:
                cli.getConsole().println("No features were found in the database.");
                break;
            case TABLE_NOT_FOUND:
                cli.getConsole().println("Could not find the specified table.");
                throw new CommandFailedException();
            case UNABLE_TO_GET_NAMES:
                cli.getConsole().println("Unable to get feature types from the database.");
                throw new CommandFailedException();
            case UNABLE_TO_GET_FEATURES:
                cli.getConsole().println("Unable to get features from the database.");
                break;
            case UNABLE_TO_INSERT:
                cli.getConsole().println("Unable to insert features into the working tree.");
                throw new CommandFailedException();
            case ALTER_AND_ALL_DEFINED:
                cli.getConsole().println(
                        "Alter cannot be used with --all option and more than one table.");
                throw new CommandFailedException();
            default:
                cli.getConsole().println("Import failed with exception: " + e.statusCode.name());
                throw new CommandFailedException();
            }
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }

}
