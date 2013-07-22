package org.geogit.storage.bdbje;

import java.util.HashSet;
import java.util.Set;

import org.geogit.storage.DeduplicationService;
import org.geogit.storage.Deduplicator;

import com.google.inject.Inject;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;

public class BDBJEDeduplicationService implements DeduplicationService {
    private EnvironmentBuilder environmentBuilder;
    private Set<BDBJEDeduplicator> openDeduplicators = new HashSet<BDBJEDeduplicator>();
    private volatile Environment environment;
    private volatile int tick = 0;

    @Inject
    public BDBJEDeduplicationService(EnvironmentBuilder environmentBuilder) {
        this.environmentBuilder = environmentBuilder;
    }
    
    private synchronized Environment getEnvironment() {
        if (this.environment == null) {
            this.environment = environmentBuilder.setRelativePath("seen").get();
        }
        return this.environment;
    }
    
    private synchronized Database createDatabase() {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setDeferredWrite(false);
        dbConfig.setTransactional(false);
        dbConfig.setTemporary(true);

        return getEnvironment().openDatabase(null, "seen" + (tick++), dbConfig);
    }
    
    @Override
    public synchronized Deduplicator createDeduplicator() {
    	Database database = createDatabase();
        BDBJEDeduplicator deduplicator = new BDBJEDeduplicator(database, this);
        this.openDeduplicators.add(deduplicator);
        return deduplicator;
    }
    
    protected void reset(BDBJEDeduplicator deduplicator) {
    	deduplicator.setDatabase(createDatabase());
    }
    
    public synchronized void deregister(BDBJEDeduplicator deduplicator) {
        this.openDeduplicators.remove(deduplicator);
        if (this.openDeduplicators.size() == 0) {
            this.environment.close();
            this.environment = null;
        }
    }
}
