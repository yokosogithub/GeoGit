/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.porcelain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;

import com.beust.jcommander.internal.Maps;

/**
 * A template for shapefile commands; provides out of the box support for the --help argument so
 * far.
 * 
 * @see CLICommand
 */
public abstract class AbstractShpCommand extends AbstractCommand implements CLICommand {

    /**
     * Factory for constructing the data store.
     * 
     * @see ShapefileDataStoreFactory
     */
    public AbstractDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

    /**
     * Constructs a new shapefile data store using the specified shapefile.
     * 
     * @param shapefile the filepath of the shapefile to use in creating the data store
     * @return the constructed data store
     * @throws IllegalArgumentException if the datastore cannot be acquired
     * @see DataStore
     */
    protected DataStore getDataStore(String shapefile) {
        File file = new File(shapefile);
        checkParameter(file.exists(), "File does not exist '%s'", shapefile);

        try {
            Map<String, Serializable> params = Maps.newHashMap();
            params.put(ShapefileDataStoreFactory.URLP.key, new File(shapefile).toURI().toURL());
            params.put(ShapefileDataStoreFactory.NAMESPACEP.key, "http://www.opengis.net/gml");
            params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.FALSE);
            params.put(ShapefileDataStoreFactory.ENABLE_SPATIAL_INDEX.key, Boolean.FALSE);
            params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.FALSE);

            DataStore dataStore = dataStoreFactory.createDataStore(params);
            checkParameter(dataStore != null, "Unable to open '%s' as a shapefile", shapefile);

            return dataStore;
        } catch (IOException e) {
            throw new CommandFailedException("Error opening shapefile: " + e.getMessage(), e);
        }
    }
}