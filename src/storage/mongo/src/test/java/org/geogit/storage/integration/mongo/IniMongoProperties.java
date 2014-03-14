/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.integration.mongo;

import org.geogit.test.integration.OnlineTestProperties;

public class IniMongoProperties extends OnlineTestProperties {

    public IniMongoProperties() {
        super(".geogit-mongo-tests.properties", "mongodb.uri", "mongodb://localhost:27017/",
                "mongodb.database", "geogit");
    }
}
