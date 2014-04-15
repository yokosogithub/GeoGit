/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.blueprints;

import org.geogit.api.TestPlatform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.GraphDatabaseStressTest;
import org.geogit.storage.fs.IniFileConfigDatabase;

public class TinkerGraphDatabaseStressTest extends GraphDatabaseStressTest {

    @Override
    protected GraphDatabase createDatabase(TestPlatform platform) {
        ConfigDatabase configDB = new IniFileConfigDatabase(platform);
        return new TinkerGraphDatabase(platform, configDB);
    }
}
