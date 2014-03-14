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
 * Imports one or more tables from a SpatiaLite database.
 * 
 * SpatiaLite CLI proxy for {@link ImportOp}
 * 
 * @see ImportOp
 */
@ObjectDatabaseReadOnly
@Parameters(commandNames = "import", commandDescription = "Import SpatiaLite database")
public class SLImport extends AbstractSLCommand implements CLICommand {

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
     * do not replace or delete features
     */
    @Parameter(names = { "--add" }, description = "Do not replace or delete features on the destination path, but just add new ones")
    boolean add;

    /**
     * Set the path default feature type to the the feature type of imported features, and modify
     * existing features to match it
     */
    @Parameter(names = { "--alter" }, description = "Set the path default feature type to the the feature type of imported features, and modify existing features to match it")
    boolean alter;

    /**
     * Executes the import command using the provided options.
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        DataStore dataStore = getDataStore();

        try {

            cli.getConsole().println("Importing from database " + commonArgs.database);

            ProgressListener progressListener = cli.getProgressListener();
            cli.getGeogit().command(ImportOp.class).setAll(all).setTable(table).setAlter(alter)
                    .setOverwrite(!add).setDataStore(dataStore)
                    .setProgressListener(progressListener).call();

            cli.getConsole().println("Import successful.");

        } catch (GeoToolsOpException e) {
            switch (e.statusCode) {
            case TABLE_NOT_DEFINED:
                throw new CommandFailedException(
                        "No tables specified for import. Specify --all or --table <table>.", e);
            case ALL_AND_TABLE_DEFINED:
                throw new CommandFailedException(
                        "Specify --all or --table <table>, both cannot be set.", e);
            case NO_FEATURES_FOUND:
                throw new CommandFailedException("No features were found in the database.", e);
            case TABLE_NOT_FOUND:
                throw new CommandFailedException("Could not find the specified table.", e);
            case UNABLE_TO_GET_NAMES:
                throw new CommandFailedException("Unable to get feature types from the database.",
                        e);
            case UNABLE_TO_GET_FEATURES:
                throw new CommandFailedException("Unable to get features from the database.", e);
            case UNABLE_TO_INSERT:
                throw new CommandFailedException(
                        "Unable to insert features into the working tree.", e);
            case ALTER_AND_ALL_DEFINED:
                throw new CommandFailedException(
                        "Alter cannot be used with --all option and more than one table.", e);
            default:
                throw new CommandFailedException("Import failed with exception: "
                        + e.statusCode.name(), e);
            }
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}
