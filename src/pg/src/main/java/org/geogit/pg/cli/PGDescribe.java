/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.pg.cli;

import java.net.ConnectException;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.storage.hessian.GeoToolsRevFeatureType;

import org.geotools.data.DataStore;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.jdbc.JDBCDataStore;

import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 *
 */
@Parameters(commandNames = "describe", commandDescription = "Describe a PostGIS table")
public class PGDescribe extends AbstractPGCommand implements CLICommand {

    @ParametersDelegate
    public PGDescribeArgs args = new PGDescribeArgs();

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }
        if (args.table.isEmpty()) {
            cli.getConsole().println("No table supplied");
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

            doDescribe(cli, dataStore);
        } catch (ConnectException e) {
            cli.getConsole().println("Unable to connect using the specified database parameters.");
        } finally {
            if (dataStore != null) {
                dataStore.dispose();
            }
            cli.getConsole().flush();
        }
    }

    private void doDescribe(GeogitCLI cli, DataStore dataStore) throws Exception {
        final ConsoleReader console = cli.getConsole();

        console.println("Fetching table...");

        boolean foundTable = false;

        List<Name> typeNames = dataStore.getNames();
        for (Name typeName : typeNames) {
            if (!args.table.equals(typeName.toString()))
                continue;

            foundTable = true;

            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
            GeoToolsRevFeatureType revType = new GeoToolsRevFeatureType(featureSource.getSchema());

            ResourceInfo info = featureSource.getInfo();
            console.println("Table : " + info.getName());
            Collection<PropertyDescriptor> descriptors = revType.type().getDescriptors();
            console.println("----------------------------------------");
            for (PropertyDescriptor descriptor : descriptors) {
                console.println("\tProperty  : " + descriptor.getName());
                console.println("\tType      : "
                        + descriptor.getType().getBinding().getSimpleName());
                console.println("----------------------------------------");
            }
        }

        if (!foundTable) {
            console.println("Could not find the specified table.");
        }
    }
}
