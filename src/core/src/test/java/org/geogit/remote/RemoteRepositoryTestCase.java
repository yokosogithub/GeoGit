/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.InjectorBuilder;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.Remote;
import org.geogit.api.RevCommit;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.LsRemote;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CloneOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.FetchOp;
import org.geogit.api.porcelain.PullOp;
import org.geogit.api.porcelain.PushOp;
import org.geogit.repository.Repository;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.DeduplicationService;
import org.geogit.test.integration.TestInjectorBuilder;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.inject.Injector;
import com.vividsolutions.jts.io.ParseException;

public abstract class RemoteRepositoryTestCase {

    protected static final String idL1 = "Lines.1";

    protected static final String idL2 = "Lines.2";

    protected static final String idL3 = "Lines.3";

    protected static final String idP1 = "Points.1";

    protected static final String idP2 = "Points.2";

    protected static final String idP3 = "Points.3";

    protected static final String pointsNs = "http://geogit.points";

    protected static final String pointsName = "Points";

    protected static final String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected static final Name pointsTypeName = new NameImpl("http://geogit.points", pointsName);

    protected SimpleFeatureType pointsType;

    protected Feature points1;

    protected Feature points1_modified;

    protected Feature points2;

    protected Feature points3;

    protected static final String linesNs = "http://geogit.lines";

    protected static final String linesName = "Lines";

    protected static final String linesTypeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";

    protected static final Name linesTypeName = new NameImpl("http://geogit.lines", linesName);

    protected SimpleFeatureType linesType;

    protected Feature lines1;

    protected Feature lines2;

    protected Feature lines3;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    protected class GeogitContainer {
        public GeoGIT geogit;

        public Repository repo;

        public File envHome;

        public Injector injector;

        public GeogitContainer(final String workingDirectory) throws IOException {

            envHome = tempFolder.newFolder(workingDirectory);

            InjectorBuilder injectorBuilder = createInjectorBuilder();
            GlobalInjectorBuilder.builder = injectorBuilder;
            injector = injectorBuilder.build();

            geogit = new GeoGIT(injector, envHome);
            repo = geogit.getOrCreateRepository();

            repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                    .setValue("Gabriel Roldan").call();
            repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                    .setValue("groldan@opengeo.org").call();
        }

        public void tearDown() throws IOException {
            if (repo != null) {
                repo.close();
            }
            repo = null;
            injector = null;
        }

        public InjectorBuilder createInjectorBuilder() {
            Platform testPlatform = new TestPlatform(envHome);
            return new TestInjectorBuilder(testPlatform);
        }
    }

    public GeogitContainer localGeogit;

    public GeogitContainer remoteGeogit;

    public IRemoteRepo remoteRepo;

    // prevent recursion
    private boolean setup = false;

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
        localGeogit = new GeogitContainer("localtestrepository");
        remoteGeogit = new GeogitContainer("remotetestrepository");

        LocalRemoteRepo remoteRepo = spy(new LocalRemoteRepo(remoteGeogit.createInjectorBuilder()
                .build(), remoteGeogit.envHome.getCanonicalFile(), localGeogit.repo));

        doNothing().when(remoteRepo).close();

        remoteRepo.setGeoGit(remoteGeogit.geogit);
        this.remoteRepo = remoteRepo;

        pointsType = DataUtilities.createType(pointsNs, pointsName, pointsTypeSpec);

        points1 = feature(pointsType, idP1, "StringProp1_1", new Integer(1000), "POINT(1 1)");
        points1_modified = feature(pointsType, idP1, "StringProp1_1a", new Integer(1001),
                "POINT(1 2)");
        points2 = feature(pointsType, idP2, "StringProp1_2", new Integer(2000), "POINT(2 2)");
        points3 = feature(pointsType, idP3, "StringProp1_3", new Integer(3000), "POINT(3 3)");

        linesType = DataUtilities.createType(linesNs, linesName, linesTypeSpec);

        lines1 = feature(linesType, idL1, "StringProp2_1", new Integer(1000),
                "LINESTRING (1 1, 2 2)");
        lines2 = feature(linesType, idL2, "StringProp2_2", new Integer(2000),
                "LINESTRING (3 3, 4 4)");
        lines3 = feature(linesType, idL3, "StringProp2_3", new Integer(3000),
                "LINESTRING (5 5, 6 6)");

        setUpInternal();
    }

    protected LsRemote lsremote() {
        LsRemote lsRemote = spy(localGeogit.geogit.command(LsRemote.class));

        doReturn(Optional.of(remoteRepo)).when(lsRemote).getRemoteRepo(any(Remote.class));

        return lsRemote;
    }

    protected FetchOp fetch() {
        FetchOp remoteRepoFetch = spy(localGeogit.geogit.command(FetchOp.class));

        doReturn(Optional.of(remoteRepo)).when(remoteRepoFetch).getRemoteRepo(any(Remote.class),
                any(DeduplicationService.class));
        LsRemote lsRemote = lsremote();
        when(remoteRepoFetch.command(LsRemote.class)).thenReturn(lsRemote);

        return remoteRepoFetch;
    }

    protected CloneOp clone() {
        CloneOp clone = spy(localGeogit.geogit.command(CloneOp.class));
        FetchOp fetch = fetch();
        when(clone.command(FetchOp.class)).thenReturn(fetch);

        LsRemote lsRemote = lsremote();
        when(clone.command(LsRemote.class)).thenReturn(lsRemote);

        return clone;
    }

    protected PullOp pull() {
        PullOp pull = spy(localGeogit.geogit.command(PullOp.class));
        FetchOp fetch = fetch();
        when(pull.command(FetchOp.class)).thenReturn(fetch);

        LsRemote lsRemote = lsremote();
        when(pull.command(LsRemote.class)).thenReturn(lsRemote);

        return pull;
    }

    protected PushOp push() {
        PushOp push = spy(localGeogit.geogit.command(PushOp.class));
        doReturn(Optional.of(remoteRepo)).when(push).getRemoteRepo(any(Remote.class));

        FetchOp fetch = fetch();
        when(push.command(FetchOp.class)).thenReturn(fetch);

        LsRemote lsRemote = lsremote();
        when(push.command(LsRemote.class)).thenReturn(lsRemote);

        return push;
    }

    @After
    public final void tearDown() throws Exception {
        setup = false;
        tearDownInternal();
        localGeogit.tearDown();
        remoteGeogit.tearDown();
        localGeogit = null;
        remoteGeogit = null;
        System.gc();
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

    protected List<RevCommit> populate(GeoGIT geogit, boolean oneCommitPerFeature,
            Feature... features) throws Exception {
        return populate(geogit, oneCommitPerFeature, Arrays.asList(features));
    }

    protected List<RevCommit> populate(GeoGIT geogit, boolean oneCommitPerFeature,
            List<Feature> features) throws Exception {

        List<RevCommit> commits = new ArrayList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(geogit, f);
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
    protected ObjectId insertAndAdd(GeoGIT geogit, Feature f) throws Exception {
        ObjectId objectId = insert(geogit, f);

        geogit.command(AddOp.class).call();
        return objectId;
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    protected ObjectId insert(GeoGIT geogit, Feature f) throws Exception {
        final WorkingTree workTree = geogit.getRepository().getWorkingTree();
        Name name = f.getType().getName();
        String parentPath = name.getLocalPart();
        Node ref = workTree.insert(parentPath, f);
        ObjectId objectId = ref.getObjectId();
        return objectId;
    }

    protected void insertAndAdd(GeoGIT geogit, Feature... features) throws Exception {
        insert(geogit, features);
        geogit.command(AddOp.class).call();
    }

    protected void insert(GeoGIT geogit, Feature... features) throws Exception {
        for (Feature f : features) {
            insert(geogit, f);
        }
    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    protected boolean deleteAndAdd(GeoGIT geogit, Feature f) throws Exception {
        boolean existed = delete(geogit, f);
        if (existed) {
            geogit.command(AddOp.class).call();
        }

        return existed;
    }

    protected boolean delete(GeoGIT geogit, Feature f) throws Exception {
        final WorkingTree workTree = geogit.getRepository().getWorkingTree();
        Name name = f.getType().getName();
        String localPart = name.getLocalPart();
        String id = f.getIdentifier().getID();
        boolean existed = workTree.delete(localPart, id);
        return existed;
    }

    protected <E> List<E> toList(Iterator<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterators.addAll(logged, logs);
        return logged;
    }

    protected <E> List<E> toList(Iterable<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterables.addAll(logged, logs);
        return logged;
    }

    /**
     * Computes the aggregated bounds of {@code features}, assuming all of them are in the same CRS
     */
    protected ReferencedEnvelope boundsOf(Feature... features) {
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
    protected ReferencedEnvelope boundsOf(CoordinateReferenceSystem targetCrs, Feature... features)
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
