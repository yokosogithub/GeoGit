package org.geogit.storage.mongo;

public class MongoAddress {
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
}
