package org.geogit.storage.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.BulkOpListener;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.datastream.DataStreamSerializationFactory;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class HBaseObjectDatabase implements ObjectDatabase {
    
    /* A non-instantiable class that manages creation of HConnections.*/
    private final HConnectionManager manager;
    
    private HConnection connection;
    
    protected ConfigDatabase config;
    // To administer HBase, create and drop tables, list and alter tables, use HBaseAdmin.
    private HBaseAdmin client = null;
    
    
    /* from mongodb */
    /*
     * private MongoClient client = null; protected DB db = null; protected DBCollection collection
     * = null;
     */
    
    protected ObjectSerializingFactory serializers = new DataStreamSerializationFactory();

    private String collectionName;

    
    @Inject
    public HBaseObjectDatabase(ConfigDatabase config, HConnectionManager manager, HConnection connection) {
        this(config, manager, connection, "objects");
    }

    HBaseObjectDatabase(ConfigDatabase config, HConnectionManager manager, HConnection connection, String collectionName) {
        this.config = config;
        this.manager = manager;
        this.connection = connection;
        this.collectionName = collectionName;
    }
    
    @Override
    public void open() {
        if (client != null) {
            return;
        }
        
        String uri = config.get("hbase.uri").get();
        String database = config.get("hbase.database").get();
        Configuration hbConfig = HBaseConfiguration.create();
        hbConfig.set("someValue", uri);
        hbConfig.set("someValue", database);
        
        try {
            connection = manager.createConnection(hbConfig);
            client = new HBaseAdmin(connection);
        } catch (ZooKeeperConnectionException e) {
            e.printStackTrace();
        } catch (MasterNotRunningException e){
            e.printStackTrace();
        }
        
        
    }
    
    @Override
    public synchronized boolean isOpen() {
        return client != null;
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.configure(config, "hbase", "0.1");
        String uri = config.get("hbase.uri").or(config.getGlobal("hbase.uri"))
                .or("hbase://localhost:2181/");
        String database = config.get("hbase.database").or(config.getGlobal("hbase.database"))
                .or("geogit");
        config.put("hbase.uri", uri);
        config.put("hbase.database", database);
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.verify(config, "hbase", "0.1");
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                connection.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        client = null;
    }

    @Override
    public boolean exists(ObjectId id) {
        Scan s = new Scan();
        s.addColumn(Bytes.toBytes("family"), Bytes.toBytes("qualifier")); // replaced "qualifier" with ObjectId id
        
        HTable table;
        Result rr = null;
        ResultScanner scanner;
        try {
            table = new HTable(Bytes.toBytes("geogitTable"), connection); // replaced with the name of the table
            scanner = table.getScanner(s);
            rr = scanner.next();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            scanner.close();
        }
        
        return (rr != null);
    }

    @Override
    public List<ObjectId> lookUp(String partialId) {
        
    }

    @Override
    public RevObject get(ObjectId id) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends RevObject> T get(ObjectId id, Class<T> type) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RevObject getIfPresent(ObjectId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends RevObject> T getIfPresent(ObjectId id, Class<T> type)
            throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RevTree getTree(ObjectId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RevFeature getFeature(ObjectId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RevFeatureType getFeatureType(ObjectId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RevCommit getCommit(ObjectId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RevTag getTag(ObjectId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean put(RevObject object) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ObjectInserter newObjectInserter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean delete(ObjectId objectId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<RevObject> getAll(Iterable<ObjectId> ids, BulkOpListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects) {
        // TODO Auto-generated method stub

    }

    @Override
    public void putAll(Iterator<? extends RevObject> objects, BulkOpListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long deleteAll(Iterator<ObjectId> ids, BulkOpListener listener) {
        // TODO Auto-generated method stub
        return 0;
    }

}