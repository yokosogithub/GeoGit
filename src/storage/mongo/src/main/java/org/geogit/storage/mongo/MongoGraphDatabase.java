package org.geogit.storage.mongo;

import org.geogit.api.Platform;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.BlueprintsGraphDatabase;
import org.geogit.storage.ConfigDatabase;

import com.boundlessgeo.blongo.MongoGraph;
import com.google.inject.Inject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

/**
 * A graph database that uses a MongoDB server for persistence.
 */
public class MongoGraphDatabase extends BlueprintsGraphDatabase<MongoGraph> {
    private final MongoConnectionManager manager;

    private final ConfigDatabase config;

    @Inject
    public MongoGraphDatabase(final Platform platform, final MongoConnectionManager manager,
            final ConfigDatabase config) {
        super(platform);
        this.config = config;
        this.manager = manager;
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.GRAPH.configure(config, "mongodb", "0.1");
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.GRAPH.verify(config, "mongodb", "0.1");
    }

    @Override
    protected MongoGraph getGraphDatabase() {
        String hostname = config.get("mongo.host").get();
        int port = config.get("mongo.port", Integer.class).get();
        MongoClient client = manager.acquire(new MongoAddress(hostname, port));
        DB db = client.getDB("geogit");
        DBCollection collection = db.getCollection("graph");
        return new MongoGraph(collection);
    }

}
