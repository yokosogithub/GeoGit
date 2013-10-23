/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.mongo;

import com.google.common.base.Objects;

/**
 * A value object containing connection info for Mongo databases.
 * These are used as keys for the connection managers.
 * 
 * @see MongoObjectDatabase
 * @see MongoGraphDatabase
 * @see MongoStagingDatabase
 */
final class MongoAddress {
    private final String host;
    private final int port;

    public MongoAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(host, port);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(MongoAddress.class).addValue(host)
                .addValue(port).toString();
    }
}
