package org.geogit.geotools.data.stresstest;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.InitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.cli.test.functional.CLITestInjectorBuilder;
import org.geogit.geotools.data.GeoGitDataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;

public class DataStoreConcurrencyTest {

    private GeoGitDataStore store;

    private static final SimpleFeatureType pointType;
    static {
        final String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";
        try {
            pointType = DataUtilities.createType("point", pointsTypeSpec);
        } catch (SchemaException e) {
            throw Throwables.propagate(e);
        }
    }

    private ExecutorService editThreads;

    private ExecutorService readThreads;

    private final int writeThreadCount = 4, readThreadCount = 4;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private int initialCommitCount;

    @Before
    public void beforeTest() throws Exception {

        File workingDirectory = tmp.newFolder("repo");
        File userHomeDirectory = tmp.newFolder("home");
        TestPlatform platform = new TestPlatform(workingDirectory);
        platform.setUserHome(userHomeDirectory);
        Injector injector = new CLITestInjectorBuilder(platform).build();
        GeoGIT geogit = new GeoGIT(injector);
        geogit.command(InitOp.class).call();
        geogit.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("gabriel").call();
        geogit.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("gabriel@roldan.example.com").call();

        store = new GeoGitDataStore(geogit);

        store.createSchema(pointType);

        editThreads = Executors.newFixedThreadPool(writeThreadCount, new ThreadFactoryBuilder()
                .setNameFormat("edit-thread-%d").build());

        readThreads = Executors.newFixedThreadPool(readThreadCount, new ThreadFactoryBuilder()
                .setNameFormat("read-thread-%d").build());
        initialCommitCount = copyOf(store.getGeogit().command(LogOp.class).call()).size();
    }

    @After
    public void afterTest() throws Exception {
        if (store != null) {
            store.dispose();
        }
        if (editThreads != null) {
            editThreads.shutdownNow();
        }
        if (readThreads != null) {
            readThreads.shutdownNow();
        }
    }

    @Test
    public void testConcurrentEdits() throws Exception {

        final int insertsPerTask = 20;
        List<Future<Integer>> insertResults = runInserts(writeThreadCount, insertsPerTask);
        for (Future<Integer> f : insertResults) {
            assertEquals(insertsPerTask, f.get().intValue());
        }

        List<RevCommit> commits = copyOf(store.getGeogit().command(LogOp.class).call());
        final int expectedCommitCount = initialCommitCount + insertsPerTask * writeThreadCount;
        assertEquals(expectedCommitCount, commits.size());
    }

    @Test
    public void testConcurrentReads() throws Exception {

        final int insertsPerTask = 20;
        assertEquals(insertsPerTask, runInserts(1, insertsPerTask).get(0).get().intValue());

        final int readsPerTask = 20;
        List<Future<Integer>> readResults = runReads(readThreadCount, readsPerTask);

        for (Future<Integer> f : readResults) {
            assertEquals(readsPerTask, f.get().intValue());
        }
    }

    @Test
    public void testConcurrentEditsAndReads() throws Exception {

        final int insertsPerTask = 40;
        final int readsPerTask = 200;

        // have something to read
        runInserts(1, insertsPerTask).get(0).get();

        List<Future<Integer>> insertResults = runInserts(writeThreadCount, insertsPerTask);
        Thread.sleep(3000);
        List<Future<Integer>> readResults = runReads(readThreadCount, readsPerTask);

        for (Future<Integer> f : insertResults) {
            assertEquals(insertsPerTask, f.get().intValue());
        }
        for (Future<Integer> f : readResults) {
            assertEquals(readsPerTask, f.get().intValue());
        }

        List<RevCommit> commits = copyOf(store.getGeogit().command(LogOp.class).call());
        final int expectedCommitCount = insertsPerTask + initialCommitCount + insertsPerTask
                * writeThreadCount;
        assertEquals(expectedCommitCount, commits.size());
    }

    private List<Future<Integer>> runInserts(final int writeThreadCount, final int insertsPerTask) {
        List<Future<Integer>> insertResults = Lists.newArrayList();
        for (int i = 0; i < writeThreadCount; i++) {
            insertResults.add(editThreads.submit(new InsertTask(store, insertsPerTask)));
        }
        return insertResults;
    }

    private List<Future<Integer>> runReads(final int readThreadCount, final int readsPerTask) {
        List<Future<Integer>> readResults = Lists.newArrayList();
        for (int i = 0; i < readThreadCount; i++) {
            readResults.add(readThreads.submit(new ReadTask(store, readsPerTask)));
        }
        return readResults;
    }

    public static class InsertTask implements Callable<Integer> {

        private static final Random rnd = new Random(1000);

        private final GeoGitDataStore dataStore;

        private final SimpleFeatureBuilder builder;

        private int numInserts;

        public InsertTask(GeoGitDataStore store, int numInserts) {
            this.dataStore = store;
            this.numInserts = numInserts;
            this.builder = new SimpleFeatureBuilder(pointType);
        }

        @Override
        public Integer call() {
            int random;
            synchronized (rnd) {
                random = rnd.nextInt();
            }
            final String typeName = pointType.getTypeName();
            SimpleFeatureStore featureSource;
            int insertCount = 0;
            try {
                for (int i = 0; i < numInserts; i++) {
                    builder.reset();
                    builder.set("sp", String.valueOf(random));
                    builder.set("ip", Integer.valueOf(random));
                    SimpleFeature feature = builder.buildFeature(String.valueOf(random));

                    featureSource = (SimpleFeatureStore) dataStore.getFeatureSource(typeName);
                    Transaction tx = new DefaultTransaction();
                    featureSource.setTransaction(tx);
                    try {
                        featureSource.addFeatures(DataUtilities.collection(feature));
                        tx.commit();
                        insertCount++;
                    } finally {
                        tx.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw Throwables.propagate(e);
            }
            System.err.printf("Thread %s finished\n", Thread.currentThread().getName());
            return insertCount;
        }
    }

    public static class ReadTask implements Callable<Integer> {

        private final GeoGitDataStore dataStore;

        private final int numReads;

        public ReadTask(GeoGitDataStore store, final int numReads) {
            this.dataStore = store;
            this.numReads = numReads;
        }

        @Override
        public Integer call() {
            int readCount = 0;
            try {
                for (int i = 0; i < numReads; i++) {
                    doRead();
                    readCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw Throwables.propagate(e);
            }
            System.err.printf("Thread %s finished\n", Thread.currentThread().getName());
            return readCount;
        }

        private void doRead() throws IOException {
            final String typeName = pointType.getTypeName();
            SimpleFeatureSource featureSource;
            featureSource = dataStore.getFeatureSource(typeName);
            SimpleFeatureCollection fc = featureSource.getFeatures();
            SimpleFeatureIterator features = fc.features();
            while (features.hasNext()) {
                SimpleFeature next = features.next();
            }
            features.close();
        }
    }

    public static void main(String args[]) {
        DataStoreConcurrencyTest test = new DataStoreConcurrencyTest();
        try {
            test.tmp.create();
            test.beforeTest();
            test.testConcurrentEditsAndReads();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                test.afterTest();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
