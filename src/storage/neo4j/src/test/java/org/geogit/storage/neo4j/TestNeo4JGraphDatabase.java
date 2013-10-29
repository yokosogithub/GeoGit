/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.neo4j;

import java.util.HashMap;
import java.util.Map;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.inject.Inject;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

public class TestNeo4JGraphDatabase extends Neo4JGraphDatabase {

    @Inject
    public TestNeo4JGraphDatabase(Platform platform, ConfigDatabase configDB) {
        super(platform, configDB);
    }

    @Override
    protected Neo4jGraph getGraphDatabase() {
        TestGraphDatabaseFactory factory = new TestGraphDatabaseFactory();
        Map<String, String> settings = new HashMap<String, String>();
        GraphDatabaseService service = factory.newImpermanentDatabaseBuilder().setConfig(settings)
                .newGraphDatabase();
        return new Neo4jGraph(service);
    }

    @Override
    protected void destroyGraphDatabase() {
        try {
            graphDB.shutdown();
        } finally {
            databaseServices.remove(dbPath);
        }
    }
}
