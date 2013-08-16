/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.Platform;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.inject.Inject;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

public class TestNeo4JGraphDatabase extends Neo4JGraphDatabase {

    @Inject
    public TestNeo4JGraphDatabase(Platform platform) {
        super(platform);
    }

    @Override
    protected Neo4jGraph getGraphDatabase() {
    	return new Neo4jGraph(
             new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase());
    }

    @Override
    protected void destroyGraphDatabase() {
        graphDB.shutdown();
        databaseServices.remove(dbPath);
    }
}
