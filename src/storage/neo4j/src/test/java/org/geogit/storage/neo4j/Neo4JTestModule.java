/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage.neo4j;

import org.geogit.api.Platform;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.memory.HeapStagingDatabase;

import com.google.inject.AbstractModule;

/**
 * Guice module with tweaks to run functional tests on the target {@link Platform}'s working
 * directory.
 * 
 * @see CLITestInjectorBuilder
 */
public class Neo4JTestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(StagingDatabase.class).to(HeapStagingDatabase.class);
        bind(GraphDatabase.class).to(TestNeo4JGraphDatabase.class);
    }

}
