package org.geogit.storage.memory;

import org.geogit.api.Platform;
import org.geogit.storage.GraphDatabaseTest;

public class HeapGraphDatabaseTest extends GraphDatabaseTest {

    @Override
    protected HeapGraphDatabase createDatabase(Platform platform) {
        return new HeapGraphDatabase(platform);
    }

}
