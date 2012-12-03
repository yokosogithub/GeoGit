/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

import com.google.common.collect.AbstractIterator;
import com.google.inject.Inject;

/**
 * Internal operation for importing tables from a GeoTools {@link DataStore}.
 * 
 * @see DataStore
 */
public class ImportOp extends AbstractGeoGitOp<RevTree> {

    private boolean all = false;

    private String table = null;

    private DataStore dataStore;

    private WorkingTree workTree;

    /**
     * Constructs a new import operation with the given working tree.
     * 
     * @param workTree the working tree where features will be imported to
     */
    @Inject
    public ImportOp(final WorkingTree workTree) {
        this.workTree = workTree;
    }

    /**
     * Executes the import operation using the parameters that have been specified. Features will be
     * added to the working tree, and a new working tree will be constructed. Either {@code all} or
     * {@code table}, but not both, must be set prior to the import process.
     * 
     * @return RevTree the new working tree
     */
    @Override
    public RevTree call() {
        if (dataStore == null) {
            throw new GeoToolsOpException(StatusCode.DATASTORE_NOT_DEFINED);
        }

        if ((table == null || table.isEmpty()) && all == false) {
            throw new GeoToolsOpException(StatusCode.TABLE_NOT_DEFINED);
        }

        if (table != null && !table.isEmpty() && all == true) {
            throw new GeoToolsOpException(StatusCode.ALL_AND_TABLE_DEFINED);
        }

        boolean foundTable = false;

        List<Name> typeNames;
        try {
            typeNames = dataStore.getNames();
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_NAMES);
        }

        for (Name typeName : typeNames) {
            if (!all && !table.equals(typeName.toString()))
                continue;

            foundTable = true;

            SimpleFeatureSource featureSource;
            SimpleFeatureCollection features;
            try {
                featureSource = dataStore.getFeatureSource(typeName);
                features = featureSource.getFeatures();
            } catch (Exception e) {
                throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_FEATURES);
            }

            RevFeatureType revType = new RevFeatureType(featureSource.getSchema());

            String treePath = revType.getName().getLocalPart();

            final SimpleFeatureIterator featureIterator = features.features();

            Iterator<Feature> iterator = new AbstractIterator<Feature>() {
                @Override
                protected Feature computeNext() {
                    if (!featureIterator.hasNext()) {
                        return super.endOfData();
                    }
                    return featureIterator.next();
                }
            };
            ProgressListener progressListener = getProgressListener();
            try {
                Integer collectionSize = features.size();
                workTree.delete(revType.getName());
                workTree.insert(treePath, iterator, true, progressListener, null, collectionSize);
            } catch (Exception e) {
                throw new GeoToolsOpException(StatusCode.UNABLE_TO_INSERT);
            } finally {
                featureIterator.close();
            }
        }
        if (!foundTable) {
            if (all) {
                throw new GeoToolsOpException(StatusCode.NO_FEATURES_FOUND);
            } else {
                throw new GeoToolsOpException(StatusCode.TABLE_NOT_FOUND);
            }
        }
        return workTree.getTree();
    }

    /**
     * @param all if this is set, all tables from the data store will be imported
     * @return {@code this}
     */
    public ImportOp setAll(boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @return whether or not the all flag has been set
     */
    public boolean getAll() {
        return all;
    }

    /**
     * @param table if this is set, only the specified table will be imported from the data store
     * @return {@code this}
     */
    public ImportOp setTable(String table) {
        this.table = table;
        return this;
    }

    /**
     * @return the table that has been set, or null
     */
    public String getTable() {
        return table;
    }

    /**
     * @param dataStore the data store to use for the import process
     * @return {@code this}
     */
    public ImportOp setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return this;
    }

    /**
     * @return the data store that has been set
     */
    public DataStore getDataStore() {
        return dataStore;
    }
}
