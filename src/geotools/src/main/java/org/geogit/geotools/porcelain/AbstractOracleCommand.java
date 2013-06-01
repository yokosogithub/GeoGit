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
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.common.collect.Maps;

/**
 * A template for Oracle commands; provides out of the box support for the --help argument so far.
 * 
 * @see CLICommand
 */
public abstract class AbstractOracleCommand implements CLICommand {

    /**
     * Flag for displaying help for the command.
     */
    @Parameter(names = "--help", help = true, hidden = true)
    public boolean help;

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
     * Prints the correct usage of the geogit oracle command.
     */
    protected void printUsage() {
        JCommander jc = new JCommander(this);
        String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
        jc.setProgramName("geogit oracle " + commandName);
        jc.usage();
    }

    /**
     * Subclasses shall implement to do the real work, will not be called if the command was invoked
     * with {@code --help}
     */
    protected abstract void runInternal(GeogitCLI cli) throws Exception;

    /**
     * Constructs a new Oracle data store using connection parameters from {@link OracleCommonArgs}.
     * 
     * @return the constructed data store
     * @throws Exception
     * @see DataStore
     */
    protected DataStore getDataStore() throws Exception {
        Map<String, Serializable> params = Maps.newHashMap();
        params.put(OracleNGDataStoreFactory.DBTYPE.key, "oracle");
        params.put(OracleNGDataStoreFactory.HOST.key, commonArgs.host);
        params.put(OracleNGDataStoreFactory.PORT.key, commonArgs.port.toString());
        params.put(OracleNGDataStoreFactory.SCHEMA.key, commonArgs.schema);
        params.put(OracleNGDataStoreFactory.DATABASE.key, commonArgs.database);
        params.put(OracleNGDataStoreFactory.USER.key, commonArgs.username);
        params.put(OracleNGDataStoreFactory.PASSWD.key, commonArgs.password);
        //params.put(OracleNGDataStoreFactory.ESTIMATED_EXTENTS.key, commonArgs.estimatedExtent);
        //params.put(OracleNGDataStoreFactory.LOOSEBBOX.key, commonArgs.looseBbox);
//        if (!commonArgs.geometryMetadataTable.equals(""))
//            params.put(OracleNGDataStoreFactory.GEOMETRY_METADATA_TABLE.key,
//                    commonArgs.geometryMetadataTable);
        //params.put(OracleNGDataStoreFactory.FETCHSIZE.key, 1000);

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
