/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.pg.cli;

import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.net.ConnectException;

import jline.console.ConsoleReader;

import org.geogit.api.RevFeature;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.hessian.GeoToolsRevFeature;
import org.geogit.storage.hessian.GeoToolsRevFeatureType;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.jdbc.JDBCDataStore;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import com.google.common.collect.AbstractIterator;

/**
 *
 */
@Parameters(commandNames = "import", commandDescription = "Import PostGIS database")
public class PGImport extends AbstractPGCommand implements CLICommand {

    @ParametersDelegate
    public PGImportArgs args = new PGImportArgs();

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }
        if ((args.all == true && !args.table.isEmpty())
                || (args.all == false && args.table.isEmpty())) {
            cli.getConsole().println("Specify --all or --table, both cannot be set.");
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

            doImport(cli, dataStore);
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
        } finally {
            if (dataStore != null) {
                dataStore.dispose();
            }
            cli.getConsole().flush();
        }
    }

    private void doImport(GeogitCLI cli, DataStore dataStore) throws Exception {
        final ConsoleReader console = cli.getConsole();

        console.println("Importing database " + commonArgs.database);

        boolean foundTable = false;

        List<Name> typeNames = dataStore.getNames();
        for (Name typeName : typeNames) {
            if (!args.all && !args.table.equals(typeName.toString()))
                continue;

            foundTable = true;

            WorkingTree workingTree = cli.getGeogit().getRepository().getWorkingTree();

            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection features = featureSource.getFeatures();

            GeoToolsRevFeatureType revType = new GeoToolsRevFeatureType(featureSource.getSchema());

            String treePath = revType.getName().getLocalPart();

            final SimpleFeatureIterator featureIterator = features.features();

            Iterator<RevFeature> iterator = new AbstractIterator<RevFeature>() {
                @Override
                protected RevFeature computeNext() {
                    if (!featureIterator.hasNext()) {
                        return super.endOfData();
                    }
                    SimpleFeature feature = featureIterator.next();
                    return new GeoToolsRevFeature(feature);
                }
            };
            ProgressListener progressListener = cli.getProgressListener();
            try {
                Integer collectionSize = features.size();
                workingTree
                        .insert(treePath, iterator, true, progressListener, null, collectionSize);
            } finally {
                featureIterator.close();
            }
        }
        if (!foundTable) {
            if (args.all) {
                console.println("No features were found in the database.");
            } else {
                console.println("Could not find the specified table.");
            }
        }
    }
}
