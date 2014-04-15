/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.porcelain;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Maps;

/**
 * A template for PostGIS commands; provides out of the box support for the --help argument so far.
 * 
 * @see CLICommand
 */
public abstract class AbstractPGCommand extends AbstractCommand implements CLICommand {

    /**
     * Common arguments for PostGIS commands.
     * 
     * @see PGCommonArgs
     */
    @ParametersDelegate
    public PGCommonArgs commonArgs = new PGCommonArgs();

    /**
     * Factory for constructing the data store.
     * 
     * @see PostgisNGDataStoreFactory
     */
    public AbstractDataStoreFactory dataStoreFactory = new PostgisNGDataStoreFactory();

    /**
     * Constructs a new PostGIS data store using connection parameters from {@link PGCommonArgs}.
     * 
     * @return the constructed data store
     * @throws Exception
     * @see DataStore
     */
    protected DataStore getDataStore() {
        Map<String, Serializable> params = Maps.newHashMap();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
        params.put(PostgisNGDataStoreFactory.HOST.key, commonArgs.host);
        params.put(PostgisNGDataStoreFactory.PORT.key, commonArgs.port.toString());
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, commonArgs.schema);
        params.put(PostgisNGDataStoreFactory.DATABASE.key, commonArgs.database);
        params.put(PostgisNGDataStoreFactory.USER.key, commonArgs.username);
        params.put(PostgisNGDataStoreFactory.PASSWD.key, commonArgs.password);
        params.put(PostgisNGDataStoreFactory.FETCHSIZE.key, 1000);

        DataStore dataStore;
        try {
            dataStore = dataStoreFactory.createDataStore(params);
        } catch (IOException e) {
            throw new CommandFailedException(
                    "Unable to connect using the specified database parameters.", e);
        }
        if (dataStore == null) {
            throw new CommandFailedException(
                    "Unable to connect using the specified database parameters.");
        }
        if (dataStore instanceof JDBCDataStore) {
            Connection con = null;
            try {
                con = ((JDBCDataStore) dataStore).getDataSource().getConnection();
            } catch (SQLException e) {
                throw new CommandFailedException(e.getMessage(), e);
            }
            ((JDBCDataStore) dataStore).closeSafe(con);
        }

        return dataStore;
    }
}
