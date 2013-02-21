package org.geogit.api;

import org.geogit.storage.Neo4JGraphDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import com.google.inject.Inject;

public class TestNeo4JGraphDatabase extends Neo4JGraphDatabase {

    @Inject
    public TestNeo4JGraphDatabase(Platform platform) {
        super(platform);
    }

    @Override
    protected GraphDatabaseService getGraphDatabase(String dbPath) {
        return new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
    }

}
