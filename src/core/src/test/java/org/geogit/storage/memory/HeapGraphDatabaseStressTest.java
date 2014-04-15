/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import org.geogit.api.TestPlatform;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.GraphDatabaseStressTest;

public class HeapGraphDatabaseStressTest extends GraphDatabaseStressTest {

    @Override
    protected GraphDatabase createDatabase(TestPlatform platform) {
        return new HeapGraphDatabase(platform);
    }

}
