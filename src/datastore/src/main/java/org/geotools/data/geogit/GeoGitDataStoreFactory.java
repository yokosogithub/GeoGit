/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.geogit;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.repository.Repository;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;

public class GeoGitDataStoreFactory implements DataStoreFactorySpi {

    public static final Param USE_EMBEDDED_REPO = new Param("GEOGIT_EMBEDDED", Boolean.class,
            "Use Embedded GeoGIT Repository");

    public static final Param DEFAULT_NAMESPACE = new Param("namespace", String.class,
            "Default namespace", false);

    public static final Param DATA_ROOT = new Param("data_root", String.class,
            "Root directory for the versioned data store", false);

    @Override
    public String getDisplayName() {
        return "GeoGIT";
    }

    @Override
    public String getDescription() {
        return "GeoGIT Versioning DataStore";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] { USE_EMBEDDED_REPO, DEFAULT_NAMESPACE };
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            Object lookUp = USE_EMBEDDED_REPO.lookUp(params);
            return Boolean.TRUE.equals(lookUp);
        } catch (IOException e) {
            //
        }
        return false;
    }

    /**
     * @see org.geotools.data.DataAccessFactory#isAvailable()
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        Object lookUp = USE_EMBEDDED_REPO.lookUp(params);
        if (!Boolean.TRUE.equals(lookUp)) {
            throw new DataSourceException(USE_EMBEDDED_REPO.key + " is not true");
        }

        String defaultNamespace = (String) DEFAULT_NAMESPACE.lookUp(params);

        String dataRootPath = (String) DATA_ROOT.lookUp(params);
        final File dataRoot = new File(dataRootPath);

        Repository repository = new GeoGIT(dataRoot).getRepository();

        // Repository repository = GEOGIT.get().getRepository();
        GeoGitDataStore store = new GeoGitDataStore(repository, defaultNamespace);
        return store;
    }

    /**
     * @see org.geotools.data.DataStoreFactorySpi#createNewDataStore(java.util.Map)
     */
    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        throw new UnsupportedOperationException();
    }

}
