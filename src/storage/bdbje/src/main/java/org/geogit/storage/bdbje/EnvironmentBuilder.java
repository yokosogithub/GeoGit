/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class EnvironmentBuilder implements Provider<Environment> {

    @Inject
    private Platform platform;

    private String[] path;

    private File absolutePath;

    private boolean stagingDatabase;

    public EnvironmentBuilder setRelativePath(String... path) {
        this.path = path;
        this.absolutePath = null;
        return this;
    }

    public EnvironmentBuilder setAbsolutePath(File absolutePath) {
        this.absolutePath = absolutePath;
        this.path = null;
        return this;
    }

    /**
     * @return
     * @see com.google.inject.Provider#get()
     */
    @Override
    public synchronized Environment get() {

        final URL repoUrl = new ResolveGeogitDir(platform).call();
        if (repoUrl == null && absolutePath == null) {
            throw new IllegalStateException("Can't find geogit repository home");
        }
        final File storeDirectory;

        if (absolutePath != null) {
            storeDirectory = absolutePath;
        } else {
            File currDir;
            try {
                currDir = new File(repoUrl.toURI());
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
            File dir = currDir;
            for (String subdir : path) {
                dir = new File(dir, subdir);
            }
            storeDirectory = dir;
        }

        if (!storeDirectory.exists() && !storeDirectory.mkdirs()) {
            throw new IllegalStateException("Unable to create Environment directory: '"
                    + storeDirectory.getAbsolutePath() + "'");
        }
        {
            File conf = new File(storeDirectory, "je.properties");
            if (!conf.exists()) {
                String resource = stagingDatabase ? "je.properties.staging"
                        : "je.properties.objectdb";
                InputSupplier<InputStream> from = Resources.newInputStreamSupplier(getClass()
                        .getResource(resource));
                try {
                    Files.copy(from, conf);
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        }
        EnvironmentConfig envCfg;

        // use the default settings
        envCfg = new EnvironmentConfig();
        envCfg.setAllowCreate(true);
        envCfg.setCacheMode(CacheMode.MAKE_COLD);
        envCfg.setLockTimeout(5, TimeUnit.SECONDS);
        envCfg.setDurability(Durability.COMMIT_NO_SYNC);

//        // envCfg.setSharedCache(true);
//        //
//         final boolean transactional = false;
//         envCfg.setTransactional(transactional);
//         envCfg.setCachePercent(75);// Use up to 50% of the heap size for the shared db cache
//         envCfg.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, String.valueOf(256 * 1024 * 1024));
//         // check <http://www.oracle.com/technetwork/database/berkeleydb/je-faq-096044.html#35>
//         envCfg.setConfigParam("je.evictor.lruOnly", "false");
//         envCfg.setConfigParam("je.evictor.nodesPerScan", "100");
//        
//         envCfg.setConfigParam(EnvironmentConfig.CLEANER_MIN_UTILIZATION, "25");
//         envCfg.setConfigParam(EnvironmentConfig.CHECKPOINTER_HIGH_PRIORITY, "true");
//        
//         envCfg.setConfigParam(EnvironmentConfig.CLEANER_THREADS, "4");
//         // TODO: check whether we can set is locking to false
//         envCfg.setConfigParam(EnvironmentConfig.ENV_IS_LOCKING, String.valueOf(transactional));
//        
//         envCfg.setConfigParam(EnvironmentConfig.ENV_RUN_CHECKPOINTER,
//         String.valueOf(!transactional));
//         envCfg.setConfigParam(EnvironmentConfig.ENV_RUN_CLEANER, String.valueOf(!transactional));
//        
//         // envCfg.setConfigParam(EnvironmentConfig.ENV_RUN_EVICTOR, "false");

        Environment env = new Environment(storeDirectory, envCfg);
        return env;
    }

    public void setIsStagingDatabase(boolean stagingDatabase) {
        this.stagingDatabase = stagingDatabase;
    }

}
