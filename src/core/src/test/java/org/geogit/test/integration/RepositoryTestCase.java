<<<<<<< .merge_file_t4IKlb
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitTransaction;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.repository.Repository;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.inject.Injector;
import com.vividsolutions.jts.io.ParseException;

public abstract class RepositoryTestCase extends Assert {

    public static final String idL1 = "Lines.1";

    public static final String idL2 = "Lines.2";

    public static final String idL3 = "Lines.3";

    public static final String idP1 = "Points.1";

    public static final String idP2 = "Points.2";

    public static final String idP3 = "Points.3";

    public static final String idPG1 = "Polygon.1";

    public static final String idPG2 = "Polygon.2";

    public static final String idPG3 = "Polygon.3";

    public static final String pointsNs = "http://geogit.points";

    public static final String pointsName = "Points";

    public static final String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected static final String modifiedPointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326,extra:String";

    public static final Name pointsTypeName = new NameImpl("http://geogit.points", pointsName);

    protected SimpleFeatureType pointsType;

    protected SimpleFeatureType modifiedPointsType;

    protected Feature points1;

    protected Feature points1_modified;

    protected Feature points1B;

    protected Feature points1B_modified;

    protected Feature points2;

    protected Feature points3;

    public static final String linesNs = "http://geogit.lines";

    public static final String linesName = "Lines";

    public static final String linesTypeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";

    public static final Name linesTypeName = new NameImpl("http://geogit.lines", linesName);

    public SimpleFeatureType linesType;

    public Feature lines1;

    public Feature lines2;

    public Feature lines3;

    public static final String polyNs = "http://geogit.polygon";

    public static final String polyName = "Polygon";

    public static final String polyTypeSpec = "sp:String,ip:Integer,pp:Polygon:srid=4326";

    public static final Name polyTypeName = new NameImpl("http://geogit.polygon", polyName);

    public SimpleFeatureType polyType;

    public Feature poly1;

    public Feature poly2;

    public Feature poly3;

    protected GeoGIT geogit;

    protected Repository repo;

    // prevent recursion
    private boolean setup = false;

    protected File envHome;

    private Injector injector;

    @Rule
    public TemporaryFolder repositoryTempFolder = new TemporaryFolder();

    @Before
    public final void setUp() throws Exception {
        if (setup) {
            throw new IllegalStateException("Are you calling super.setUp()!?");
        }

        setup = true;
        Logging.ALL.forceMonolineConsoleOutput();
        doSetUp();
    }

    protected final void doSetUp() throws IOException, SchemaException, ParseException, Exception {
        envHome = repositoryTempFolder.getRoot();

        injector = createInjector();

        geogit = new GeoGIT(injector, envHome);
        repo = geogit.getOrCreateRepository();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("Gabriel Roldan").call();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("groldan@opengeo.org").call();

        pointsType = DataUtilities.createType(pointsNs, pointsName, pointsTypeSpec);
        modifiedPointsType = DataUtilities.createType(pointsNs, pointsName, modifiedPointsTypeSpec);

        points1 = feature(pointsType, idP1, "StringProp1_1", new Integer(1000), "POINT(1 1)");
        points1_modified = feature(pointsType, idP1, "StringProp1_1a", new Integer(1001),
                "POINT(1 2)");
        points1B = feature(modifiedPointsType, idP1, "StringProp1_1", new Integer(1000),
                "POINT(1 1)", "ExtraString");

        points1B_modified = feature(modifiedPointsType, idP1, "StringProp1_1a", new Integer(1001),
                "POINT(1 2)", "ExtraStringB");

        points2 = feature(pointsType, idP2, "StringProp1_2", new Integer(2000), "POINT(2 2)");
        points3 = feature(pointsType, idP3, "StringProp1_3", new Integer(3000), "POINT(3 3)");

        linesType = DataUtilities.createType(linesNs, linesName, linesTypeSpec);

        lines1 = feature(linesType, idL1, "StringProp2_1", new Integer(1000),
                "LINESTRING (1 1, 2 2)");
        lines2 = feature(linesType, idL2, "StringProp2_2", new Integer(2000),
                "LINESTRING (3 3, 4 4)");
        lines3 = feature(linesType, idL3, "StringProp2_3", new Integer(3000),
                "LINESTRING (5 5, 6 6)");

        polyType = DataUtilities.createType(polyNs, polyName, polyTypeSpec);

        poly1 = feature(polyType, idPG1, "StringProp3_1", new Integer(1000),
                "POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))");
        poly2 = feature(polyType, idPG2, "StringProp3_2", new Integer(2000),
                "POLYGON ((6 6, 7 7, 8 8, 9 9, 6 6))");
        poly3 = feature(polyType, idPG3, "StringProp3_3", new Integer(3000),
                "POLYGON ((11 11, 12 12, 13 13, 14 14, 11 11))");

        setUpInternal();
    }

    protected Injector createInjector() {
        Platform testPlatform = createPlatform();
        GlobalInjectorBuilder.builder = new TestInjectorBuilder(testPlatform);
        return GlobalInjectorBuilder.builder.build();
    }

    protected Platform createPlatform() {
        Platform testPlatform = new TestPlatform(envHome);
        return testPlatform;
    }

    @After
    public final void tearDown() throws Exception {
        setup = false;
        tearDownInternal();
        if (repo != null) {
            repo.close();
        }
        repo = null;
        injector = null;
    }

    /**
     * Called as the last step in {@link #setUp()}
     */
    protected abstract void setUpInternal() throws Exception;

    /**
     * Called before {@link #tearDown()}, subclasses may override as appropriate
     */
    protected void tearDownInternal() throws Exception {
        //
    }

    public Repository getRepository() {
        return repo;
    }

    public GeoGIT getGeogit() {
        return geogit;
    }

    protected Feature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, Feature... features)
            throws Exception {
        return populate(oneCommitPerFeature, Arrays.asList(features));
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, List<Feature> features)
            throws Exception {

        List<RevCommit> commits = new ArrayList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            if (oneCommitPerFeature) {
                RevCommit commit = geogit.command(CommitOp.class).call();
                commits.add(commit);
            }
        }

        if (!oneCommitPerFeature) {
            RevCommit commit = geogit.command(CommitOp.class).call();
            commits.add(commit);
        }

        return commits;
    }

    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    public ObjectId insertAndAdd(Feature f) throws Exception {
        return insertAndAdd(null, f);
    }

    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    public ObjectId insertAndAdd(GeogitTransaction transaction, Feature f) throws Exception {
        ObjectId objectId = insert(transaction, f);

        if (transaction != null) {
            transaction.command(AddOp.class).call();
        } else {
            geogit.command(AddOp.class).call();
        }
        return objectId;
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    public ObjectId insert(Feature f) throws Exception {
        return insert(null, f);
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    public ObjectId insert(GeogitTransaction transaction, Feature f) throws Exception {
        final WorkingTree workTree = (transaction != null ? transaction.getWorkingTree() : repo
                .getWorkingTree());
        Name name = f.getType().getName();
        String parentPath = name.getLocalPart();
        Node ref = workTree.insert(parentPath, f);
        ObjectId objectId = ref.getObjectId();
        return objectId;
    }

    public void insertAndAdd(Feature... features) throws Exception {
        insertAndAdd(null, features);
    }

    public void insertAndAdd(GeogitTransaction transaction, Feature... features) throws Exception {
        insert(transaction, features);
        geogit.command(AddOp.class).call();
    }

    public void insert(Feature... features) throws Exception {
        insert(null, features);
    }

    public void insert(GeogitTransaction transaction, Feature... features) throws Exception {
        for (Feature f : features) {
            insert(transaction, f);
        }
    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    public boolean deleteAndAdd(Feature f) throws Exception {
        return deleteAndAdd(null, f);
    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    public boolean deleteAndAdd(GeogitTransaction transaction, Feature f) throws Exception {
        boolean existed = delete(transaction, f);
        if (existed) {
            if (transaction != null) {
                transaction.command(AddOp.class).call();
            } else {
                geogit.command(AddOp.class).call();
            }
        }

        return existed;
    }

    public boolean delete(Feature f) throws Exception {
        return delete(null, f);
    }

    public boolean delete(GeogitTransaction transaction, Feature f) throws Exception {
        final WorkingTree workTree = (transaction != null ? transaction.getWorkingTree() : repo
                .getWorkingTree());
        Name name = f.getType().getName();
        String localPart = name.getLocalPart();
        String id = f.getIdentifier().getID();
        boolean existed = workTree.delete(localPart, id);
        return existed;
    }

    public <E> List<E> toList(Iterator<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterators.addAll(logged, logs);
        return logged;
    }

    public <E> List<E> toList(Iterable<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterables.addAll(logged, logs);
        return logged;
    }

    /**
     * Computes the aggregated bounds of {@code features}, assuming all of them are in the same CRS
     */
    public ReferencedEnvelope boundsOf(Feature... features) {
        ReferencedEnvelope bounds = null;
        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            if (bounds == null) {
                bounds = (ReferencedEnvelope) f.getBounds();
            } else {
                bounds.include(f.getBounds());
            }
        }
        return bounds;
    }

    /**
     * Computes the aggregated bounds of {@code features} in the {@code targetCrs}
     */
    public ReferencedEnvelope boundsOf(CoordinateReferenceSystem targetCrs, Feature... features)
            throws Exception {
        ReferencedEnvelope bounds = new ReferencedEnvelope(targetCrs);

        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            BoundingBox fbounds = f.getBounds();
            if (!CRS.equalsIgnoreMetadata(targetCrs, fbounds)) {
                fbounds = fbounds.toBounds(targetCrs);
            }
            bounds.include(fbounds);
        }
        return bounds;
    }
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitTransaction;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.repository.Repository;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.inject.Injector;
import com.vividsolutions.jts.io.ParseException;

public abstract class RepositoryTestCase extends Assert {

    public static final String idL1 = "Lines.1";

    public static final String idL2 = "Lines.2";

    public static final String idL3 = "Lines.3";

    public static final String idP1 = "Points.1";

    public static final String idP2 = "Points.2";

    public static final String idP3 = "Points.3";

    public static final String idPG1 = "Polygon.1";

    public static final String idPG2 = "Polygon.2";

    public static final String idPG3 = "Polygon.3";

    public static final String pointsNs = "http://geogit.points";

    public static final String pointsName = "Points";

    public static final String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected static final String modifiedPointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326,extra:String";

    public static final Name pointsTypeName = new NameImpl("http://geogit.points", pointsName);

    protected SimpleFeatureType pointsType;

    protected SimpleFeatureType modifiedPointsType;

    protected Feature points1;

    protected Feature points1_modified;

    protected Feature points1B;

    protected Feature points1B_modified;

    protected Feature points2;

    protected Feature points3;

    public static final String linesNs = "http://geogit.lines";

    public static final String linesName = "Lines";

    public static final String linesTypeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";

    public static final Name linesTypeName = new NameImpl("http://geogit.lines", linesName);

    public SimpleFeatureType linesType;

    public Feature lines1;

    public Feature lines2;

    public Feature lines3;

    public static final String polyNs = "http://geogit.polygon";

    public static final String polyName = "Polygon";

    public static final String polyTypeSpec = "sp:String,ip:Integer,pp:Polygon:srid=4326";

    public static final Name polyTypeName = new NameImpl("http://geogit.polygon", polyName);

    public SimpleFeatureType polyType;

    public Feature poly1;

    public Feature poly2;

    public Feature poly3;

    protected GeoGIT geogit;

    protected Repository repo;

    // prevent recursion
    private boolean setup = false;

    protected File envHome;

    private Injector injector;

    @Rule
    public TemporaryFolder repositoryTempFolder = new TemporaryFolder();

    @Before
    public final void setUp() throws Exception {
        if (setup) {
            throw new IllegalStateException("Are you calling super.setUp()!?");
        }

        setup = true;
        doSetUp();
    }

    protected final void doSetUp() throws IOException, SchemaException, ParseException, Exception {
        envHome = repositoryTempFolder.newFolder("repo");

        injector = createInjector();

        geogit = new GeoGIT(injector, envHome);
        repo = geogit.getOrCreateRepository();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("Gabriel Roldan").call();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("groldan@opengeo.org").call();

        pointsType = DataUtilities.createType(pointsNs, pointsName, pointsTypeSpec);
        modifiedPointsType = DataUtilities.createType(pointsNs, pointsName, modifiedPointsTypeSpec);

        points1 = feature(pointsType, idP1, "StringProp1_1", new Integer(1000), "POINT(1 1)");
        points1_modified = feature(pointsType, idP1, "StringProp1_1a", new Integer(1001),
                "POINT(1 2)");
        points1B = feature(modifiedPointsType, idP1, "StringProp1_1", new Integer(1000),
                "POINT(1 1)", "ExtraString");

        points1B_modified = feature(modifiedPointsType, idP1, "StringProp1_1a", new Integer(1001),
                "POINT(1 2)", "ExtraStringB");

        points2 = feature(pointsType, idP2, "StringProp1_2", new Integer(2000), "POINT(2 2)");
        points3 = feature(pointsType, idP3, "StringProp1_3", new Integer(3000), "POINT(3 3)");

        linesType = DataUtilities.createType(linesNs, linesName, linesTypeSpec);

        lines1 = feature(linesType, idL1, "StringProp2_1", new Integer(1000),
                "LINESTRING (1 1, 2 2)");
        lines2 = feature(linesType, idL2, "StringProp2_2", new Integer(2000),
                "LINESTRING (3 3, 4 4)");
        lines3 = feature(linesType, idL3, "StringProp2_3", new Integer(3000),
                "LINESTRING (5 5, 6 6)");

        polyType = DataUtilities.createType(polyNs, polyName, polyTypeSpec);

        poly1 = feature(polyType, idPG1, "StringProp3_1", new Integer(1000),
                "POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))");
        poly2 = feature(polyType, idPG2, "StringProp3_2", new Integer(2000),
                "POLYGON ((6 6, 7 7, 8 8, 9 9, 6 6))");
        poly3 = feature(polyType, idPG3, "StringProp3_3", new Integer(3000),
                "POLYGON ((11 11, 12 12, 13 13, 14 14, 11 11))");

        setUpInternal();
    }

    protected Injector createInjector() {
        Platform testPlatform = createPlatform();
        GlobalInjectorBuilder.builder = new TestInjectorBuilder(testPlatform);
        return GlobalInjectorBuilder.builder.build();
    }

    protected Platform createPlatform() {
        Platform testPlatform = new TestPlatform(envHome);
        return testPlatform;
    }

    @After
    public final void tearDown() throws Exception {
        setup = false;
        tearDownInternal();
        if (repo != null) {
            repo.close();
        }
        repo = null;
        injector = null;
    }

    /**
     * Called as the last step in {@link #setUp()}
     */
    protected abstract void setUpInternal() throws Exception;

    /**
     * Called before {@link #tearDown()}, subclasses may override as appropriate
     */
    protected void tearDownInternal() throws Exception {
        //
    }

    public Repository getRepository() {
        return repo;
    }

    public GeoGIT getGeogit() {
        return geogit;
    }

    protected Feature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, Feature... features)
            throws Exception {
        return populate(oneCommitPerFeature, Arrays.asList(features));
    }

    protected List<RevCommit> populate(boolean oneCommitPerFeature, List<Feature> features)
            throws Exception {

        List<RevCommit> commits = new ArrayList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(f);
            if (oneCommitPerFeature) {
                RevCommit commit = geogit.command(CommitOp.class).call();
                commits.add(commit);
            }
        }

        if (!oneCommitPerFeature) {
            RevCommit commit = geogit.command(CommitOp.class).call();
            commits.add(commit);
        }

        return commits;
    }

    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    public ObjectId insertAndAdd(Feature f) throws Exception {
        return insertAndAdd(null, f);
    }

    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    public ObjectId insertAndAdd(GeogitTransaction transaction, Feature f) throws Exception {
        ObjectId objectId = insert(transaction, f);

        if (transaction != null) {
            transaction.command(AddOp.class).call();
        } else {
            geogit.command(AddOp.class).call();
        }
        return objectId;
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    public ObjectId insert(Feature f) throws Exception {
        return insert(null, f);
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    public ObjectId insert(GeogitTransaction transaction, Feature f) throws Exception {
        final WorkingTree workTree = (transaction != null ? transaction.getWorkingTree() : repo
                .getWorkingTree());
        Name name = f.getType().getName();
        String parentPath = name.getLocalPart();
        Node ref = workTree.insert(parentPath, f);
        ObjectId objectId = ref.getObjectId();
        return objectId;
    }

    public void insertAndAdd(Feature... features) throws Exception {
        insertAndAdd(null, features);
    }

    public void insertAndAdd(GeogitTransaction transaction, Feature... features) throws Exception {
        insert(transaction, features);
        geogit.command(AddOp.class).call();
    }

    public void insert(Feature... features) throws Exception {
        insert(null, features);
    }

    public void insert(GeogitTransaction transaction, Feature... features) throws Exception {
        for (Feature f : features) {
            insert(transaction, f);
        }
    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    public boolean deleteAndAdd(Feature f) throws Exception {
        return deleteAndAdd(null, f);
    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    public boolean deleteAndAdd(GeogitTransaction transaction, Feature f) throws Exception {
        boolean existed = delete(transaction, f);
        if (existed) {
            if (transaction != null) {
                transaction.command(AddOp.class).call();
            } else {
                geogit.command(AddOp.class).call();
            }
        }

        return existed;
    }

    public boolean delete(Feature f) throws Exception {
        return delete(null, f);
    }

    public boolean delete(GeogitTransaction transaction, Feature f) throws Exception {
        final WorkingTree workTree = (transaction != null ? transaction.getWorkingTree() : repo
                .getWorkingTree());
        Name name = f.getType().getName();
        String localPart = name.getLocalPart();
        String id = f.getIdentifier().getID();
        boolean existed = workTree.delete(localPart, id);
        return existed;
    }

    public <E> List<E> toList(Iterator<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterators.addAll(logged, logs);
        return logged;
    }

    public <E> List<E> toList(Iterable<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterables.addAll(logged, logs);
        return logged;
    }

    /**
     * Computes the aggregated bounds of {@code features}, assuming all of them are in the same CRS
     */
    public ReferencedEnvelope boundsOf(Feature... features) {
        ReferencedEnvelope bounds = null;
        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            if (bounds == null) {
                bounds = (ReferencedEnvelope) f.getBounds();
            } else {
                bounds.include(f.getBounds());
            }
        }
        return bounds;
    }

    /**
     * Computes the aggregated bounds of {@code features} in the {@code targetCrs}
     */
    public ReferencedEnvelope boundsOf(CoordinateReferenceSystem targetCrs, Feature... features)
            throws Exception {
        ReferencedEnvelope bounds = new ReferencedEnvelope(targetCrs);

        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            BoundingBox fbounds = f.getBounds();
            if (!CRS.equalsIgnoreMetadata(targetCrs, fbounds)) {
                fbounds = fbounds.toBounds(targetCrs);
            }
            bounds.include(fbounds);
        }
        return bounds;
    }
}
>>>>>>> .merge_file_p5oPR8
