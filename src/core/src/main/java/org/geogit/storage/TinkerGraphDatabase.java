/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.Platform;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * Provides an implementation of a GeoGit Graph Database using TinkerGraph.
 */
public class TinkerGraphDatabase extends BlueprintsGraphDatabase<TinkerGraph> {
    private final ConfigDatabase configDB;

	/**
     * Constructs a new {@code TinkerGraphDatabase} using the given platform.
     * 
     * @param platform the platform to use.
     */
    @Inject
    public TinkerGraphDatabase(final Platform platform, final ConfigDatabase configDB) {
        super(platform);
        this.configDB = configDB;
    }

    @Override
    protected TinkerGraph getGraphDatabase() {
        return new TinkerGraph(dbPath, TinkerGraph.FileType.GML);
    }
    
    @Override
    public void configure() {
    	Optional<String> storageName = configDB.get("storage.graph");
    	Optional<String> storageVersion = configDB.get("tinkergraph.version");
    	if (storageName.isPresent() || storageVersion.isPresent()) {
    		throw new IllegalStateException("Initializing graph database when it is already initialized!");
    	}
    	configDB.put("storage.graph", "tinkergraph");
    	configDB.put("tinkergraph.version", "0.1");
    }
    
    @Override
    public void checkConfig() {
        Optional<String> storageName = configDB.get("storage.graph");
        Optional<String> storageVersion = configDB.get("tinkergraph.version");
        boolean unset = !(storageName.isPresent() || storageVersion.isPresent());
        boolean valid = 
                storageName.isPresent() && "tinkergraph".equals(storageName.get()) &&
                storageVersion.isPresent() && "0.1".equals(storageVersion.get());
        if (!(unset || valid)) {
            throw new IllegalStateException(
                    "Cannot open staging database with format: tinkergraph and version: 0.1, found format: "
                            + storageName.orNull()
                            + ", version: "
                            + storageVersion.orNull());
        }
    }
}
