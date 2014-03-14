/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.porcelain;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.ParametersDelegate;
import com.google.common.collect.Maps;

/**
 * A template for Oracle commands; provides out of the box support for the --help argument so far.
 * 
 * @see CLICommand
 */
public abstract class AbstractOracleCommand extends AbstractCommand implements CLICommand {

    /**
     * Common arguments for Oracle commands.
     * 
     * @see OracleCommonArgs
     */
    @ParametersDelegate
    public OracleCommonArgs commonArgs = new OracleCommonArgs();

    /**
     * Factory for constructing the data store.
     * 
     * @see OracleNGDataStoreFactory
     */
    public AbstractDataStoreFactory dataStoreFactory = new OracleNGDataStoreFactory();

    /**
     * Constructs a new Oracle data store using connection parameters from {@link OracleCommonArgs}.
     * 
     * @return the constructed data store
     * @throws Exception
     * @see DataStore
     */
    protected DataStore getDataStore() {
        Map<String, Serializable> params = Maps.newHashMap();
        params.put(OracleNGDataStoreFactory.DBTYPE.key, "oracle");
        params.put(OracleNGDataStoreFactory.HOST.key, commonArgs.host);
        params.put(OracleNGDataStoreFactory.PORT.key, commonArgs.port.toString());
        params.put(OracleNGDataStoreFactory.SCHEMA.key, commonArgs.schema);
        params.put(OracleNGDataStoreFactory.DATABASE.key, commonArgs.database);
        params.put(OracleNGDataStoreFactory.USER.key, commonArgs.username);
        params.put(OracleNGDataStoreFactory.PASSWD.key, commonArgs.password);
        // params.put(OracleNGDataStoreFactory.ESTIMATED_EXTENTS.key, commonArgs.estimatedExtent);
        // params.put(OracleNGDataStoreFactory.LOOSEBBOX.key, commonArgs.looseBbox);
        // if (!commonArgs.geometryMetadataTable.equals(""))
        // params.put(OracleNGDataStoreFactory.GEOMETRY_METADATA_TABLE.key,
        // commonArgs.geometryMetadataTable);
        // params.put(OracleNGDataStoreFactory.FETCHSIZE.key, 1000);

        DataStore dataStore;
        try {
            dataStore = dataStoreFactory.createDataStore(params);
        } catch (IOException e) {
            throw new CommandFailedException(
                    "Unable to connect using the specified database parameters.", e);
        }
        if (dataStore == null) {
            throw new CommandFailedException(
                    "No suitable data store found for the provided parameters");
        }

        if (dataStore instanceof JDBCDataStore) {
            Connection con = null;
            try {
                con = ((JDBCDataStore) dataStore).getDataSource().getConnection();
            } catch (Exception e) {
                throw new CommandFailedException("Error validating the database connection", e);
            }
            ((JDBCDataStore) dataStore).closeSafe(con);
        }

        return dataStore;
    }

}
