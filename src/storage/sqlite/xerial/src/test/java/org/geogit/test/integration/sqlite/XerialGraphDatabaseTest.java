/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration.sqlite;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.GraphDatabaseTest;
import org.geogit.storage.fs.IniFileConfigDatabase;
import org.geogit.storage.sqlite.XerialGraphDatabase;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class XerialGraphDatabaseTest extends GraphDatabaseTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Override
    protected GraphDatabase createDatabase(Platform platform) throws Exception {
        ConfigDatabase configdb = new IniFileConfigDatabase(platform);
        return new XerialGraphDatabase(configdb, platform);
    }
}
