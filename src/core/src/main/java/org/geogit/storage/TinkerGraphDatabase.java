/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.Platform;
import org.geogit.repository.RepositoryConnectionException;

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
        RepositoryConnectionException.StorageType.GRAPH.configure(configDB, "tinkergraph", "0.1");
    }
    
    @Override
    public void checkConfig() {
        RepositoryConnectionException.StorageType.GRAPH.verify(configDB, "tinkergraph", "0.1");
    }
}
