/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.util.Map;

import org.geogit.api.Platform;

import com.google.inject.Inject;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

/**
 * Provides an implementation of a GeoGit Graph Database using Neo4J.
 */
public class Neo4JGraphDatabase extends TransactionalBlueprintsGraphDatabase<Neo4jGraph> {
    /**
     * Constructs a new {@code Neo4JGraphDatabase} using the given platform.
     * 
     * @param platform the platform to use.
     */
    @Inject
    public Neo4JGraphDatabase(final Platform platform) {
    	super(platform);
    }
    
    protected Neo4jGraph getGraphDatabase() {
        Map<String, String> settings = new java.util.HashMap<String, String>();
        settings.put("online_backup_enabled", "false");
        return new Neo4jGraph(dbPath, settings);
    }
}
