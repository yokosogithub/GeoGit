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
import org.geotools.data.sqlserver.SQLServerDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Maps;

/**
 * A template for Sql Server commands; provides out of the box support for the --help argument so
 * far.
 * 
 * @see CLICommand
 */
public abstract class AbstractSQLServerCommand extends AbstractCommand implements CLICommand {

    /**
     * Common arguments for SQL Server commands.
     * 
     * @see PGCommonArgs
     */
    @ParametersDelegate
    public SQLServerCommonArgs commonArgs = new SQLServerCommonArgs();

    /**
     * Factory for constructing the data store.
     * 
     * @see SQLServerDataStoreFactory
     */
    public AbstractDataStoreFactory dataStoreFactory = new SQLServerDataStoreFactory();

    /**
     * Constructs a new SQL Server data store using connection parameters from
     * {@link SQLServerCommonArgs}.
     * 
     * @return the constructed data store
     * @throws CommandFailedException
     * @see DataStore
     */
    protected DataStore getDataStore() {
        Map<String, Serializable> params = Maps.newHashMap();
        params.put(SQLServerDataStoreFactory.DBTYPE.key, "sqlserver");
        params.put(SQLServerDataStoreFactory.HOST.key, commonArgs.host);
        params.put(SQLServerDataStoreFactory.PORT.key, commonArgs.port.toString());
        params.put(SQLServerDataStoreFactory.INTSEC.key, commonArgs.intsec);
        params.put(SQLServerDataStoreFactory.SCHEMA.key, commonArgs.schema);
        params.put(SQLServerDataStoreFactory.DATABASE.key, commonArgs.database);
        params.put(SQLServerDataStoreFactory.USER.key, commonArgs.username);
        params.put(SQLServerDataStoreFactory.PASSWD.key, commonArgs.password);
        params.put(SQLServerDataStoreFactory.FETCHSIZE.key, 1000);
        if (!commonArgs.geometryMetadataTable.equals(""))
            params.put(SQLServerDataStoreFactory.GEOMETRY_METADATA_TABLE.key,
                    commonArgs.geometryMetadataTable);

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