package org.geogit.storage.neo4j;

import org.geogit.storage.GraphDatabase;

import com.google.inject.AbstractModule;

public class Neo4JModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(GraphDatabase.class).to(Neo4JGraphDatabase.class);
    }

}
