/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.neo4j;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.GraphDatabaseTest;
import org.geogit.storage.fs.IniFileConfigDatabase;

public class Neo4JGraphDatabaseTest extends GraphDatabaseTest {

    @Override
    protected GraphDatabase createDatabase(Platform platform) throws Exception {
        ConfigDatabase configDB = new IniFileConfigDatabase(platform);
        TestNeo4JGraphDatabase graphDatabase = new TestNeo4JGraphDatabase(platform, configDB);
        return graphDatabase;
    }
}
