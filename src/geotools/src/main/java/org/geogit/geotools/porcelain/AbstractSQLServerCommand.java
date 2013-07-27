/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.porcelain;

import java.io.Serializable;
import java.net.ConnectException;
import java.sql.Connection;
import java.util.Map;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.sqlserver.SQLServerDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Maps;

/**
 * A template for Sql Server commands; provides out of the box support for the --help argument so
 * far.
 * 
 * @see CLICommand
 */
public abstract class AbstractSQLServerCommand implements CLICommand {

    /**
     * Flag for displaying help for the command.
     */
    @Parameter(names = "--help", help = true, hidden = true)
    public boolean help;

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
     * Executes the command.
     * 
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        if (help) {
            printUsage();
            return;
        }

        runInternal(cli);
    }

    /**
     * Prints the correct usage of the geogit sqlserver command.
     */
    protected void printUsage() {
        JCommander jc = new JCommander(this);
        String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
        jc.setProgramName("geogit sqlserver " + commandName);
        jc.usage();
    }

    /**
     * Subclasses shall implement to do the real work, will not be called if the command was invoked
     * with {@code --help}
     */
    protected abstract void runInternal(GeogitCLI cli) throws Exception;

    /**
     * Constructs a new SQL Server data store using connection parameters from
     * {@link SQLServerCommonArgs}.
     * 
     * @return the constructed data store
     * @throws Exception
     * @see DataStore
     */
    protected DataStore getDataStore() throws Exception {
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

        DataStore dataStore = dataStoreFactory.createDataStore(params);

        if (dataStore == null) {
            throw new ConnectException();
        }

        if (dataStore instanceof JDBCDataStore) {
            Connection con = null;
            try {
                con = ((JDBCDataStore) dataStore).getDataSource().getConnection();
            } catch (Exception e) {
                throw new ConnectException();
            }

            ((JDBCDataStore) dataStore).closeSafe(con);
        }

        return dataStore;
    }
}