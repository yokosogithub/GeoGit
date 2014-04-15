/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.blueprints;

import static org.junit.Assert.assertTrue;

import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.GraphDatabaseTest;
import org.geogit.storage.fs.IniFileConfigDatabase;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class TinkerGraphDatabaseTest extends GraphDatabaseTest {

    @Override
    protected GraphDatabase createDatabase(Platform platform) throws Exception {
        ConfigDatabase configDB = new IniFileConfigDatabase(platform);
        return new TinkerGraphDatabase(platform, configDB);
    }

    @Test
    public void testPersistence() throws Exception {
        GraphDatabase db1 = super.database;

        ObjectId id = ObjectId.forString("someid");
        ImmutableList<ObjectId> parentIds = ImmutableList.of(ObjectId.forString("p1"),
                ObjectId.forString("p2"));

        assertTrue(db1.put(id, parentIds));

        IniFileConfigDatabase configDB = new IniFileConfigDatabase(platform);
        GraphDatabase db2 = new TinkerGraphDatabase.Impl(platform, configDB);
        // clear the static registry of open databases so it doesn't reuse the graph from db1
        BlueprintsGraphDatabase.databaseServices.clear();
        db2.open();
        assertTrue(db2.exists(id));
    }
}
