/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage.integration.mongo;

import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.neo4j.TestNeo4JGraphDatabase;
import org.geogit.storage.mongo.MongoConnectionManager;
import org.geogit.storage.mongo.MongoGraphDatabase;
import org.geogit.storage.mongo.MongoObjectDatabase;
import org.geogit.storage.mongo.MongoStagingDatabase;
import org.geogit.storage.integration.mongo.TestConfigDatabase;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class MongoTestStorageModule extends AbstractModule {
    @Override
    protected void configure() {
        // Mongo bindings for the different kinds of databases
        bind(MongoConnectionManager.class).in(Scopes.SINGLETON);
        bind(ObjectDatabase.class).to(MongoObjectDatabase.class).in(
                Scopes.SINGLETON);
        bind(StagingDatabase.class).to(MongoStagingDatabase.class).in(
                Scopes.SINGLETON);
        bind(GraphDatabase.class).to(MongoGraphDatabase.class).in(Scopes.SINGLETON);
        bind(ConfigDatabase.class).to(TestConfigDatabase.class);
    }
}
