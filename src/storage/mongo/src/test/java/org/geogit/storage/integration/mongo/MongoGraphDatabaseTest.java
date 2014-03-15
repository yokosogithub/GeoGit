/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.integration.mongo;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabaseTest;
import org.geogit.storage.mongo.MongoConnectionManager;
import org.geogit.storage.mongo.MongoGraphDatabase;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class MongoGraphDatabaseTest extends GraphDatabaseTest {

    @Override
    protected MongoGraphDatabase createDatabase(Platform platform) throws Exception {
        IniMongoProperties properties = new IniMongoProperties();
        String host = properties.get("mongo.host", String.class).or("localhost");
        int port = properties.get("mongodb.port", Integer.class).or(27017);
        MongoClient client = new MongoClient(host, Integer.valueOf(port));
        DB db = client.getDB("geogit");
        db.dropDatabase();

        MongoConnectionManager manager = new MongoConnectionManager();
        ConfigDatabase config = new TestConfigDatabase(platform);
        MongoGraphDatabase mongoGraphDatabase = new MongoGraphDatabase(platform, manager, config);
        return mongoGraphDatabase;
    }
}
