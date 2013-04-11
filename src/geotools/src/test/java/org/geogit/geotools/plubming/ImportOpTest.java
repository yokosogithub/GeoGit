package org.geogit.geotools.plubming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geogit.geotools.porcelain.TestHelper;
import org.geogit.repository.WorkingTree;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import cucumber.annotation.After;

public class ImportOpTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogitCLI cli;

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        setUpGeogit(cli);
    }

    @After
    public void cleanup() throws Exception {
        cli.close();
    }

    @Test
    public void testNullDataStore() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testNullTableNotAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        importOp.setAll(false);
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testEmptyTableNotAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("");
        importOp.setAll(false);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testEmptyTableAndAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("");
        importOp.setAll(true);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.call();
    }

    @Test
    public void testTableAndAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("table1");
        importOp.setAll(true);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testTableNotFound() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testNoFeaturesFound() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        importOp.setAll(true);
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testTypeNameException() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createFactoryWithGetNamesException().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testGetFeatureSourceException() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createFactoryWithGetFeatureSourceException()
                .createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testImportTable() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");

        RevTree newWorkingTree = importOp.call();
        Optional<NodeRef> ref = cli.getGeogit().command(FindTreeChild.class)
                .setParent(newWorkingTree).setChildPath("table1/table1.1").setIndex(true).call();
        assertTrue(ref.isPresent());

        ref = cli.getGeogit().command(FindTreeChild.class).setParent(newWorkingTree)
                .setChildPath("table1/table1.2").setIndex(true).call();
        assertTrue(ref.isPresent());
    }

    @Test
    public void testImportAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);

        RevTree newWorkingTree = importOp.call();
        Optional<NodeRef> ref = cli.getGeogit().command(FindTreeChild.class)
                .setParent(newWorkingTree).setChildPath("table1/table1.1").setIndex(true).call();
        assertTrue(ref.isPresent());

        ref = cli.getGeogit().command(FindTreeChild.class).setParent(newWorkingTree)
                .setChildPath("table1/table1.2").setIndex(true).call();
        assertTrue(ref.isPresent());

        ref = cli.getGeogit().command(FindTreeChild.class).setParent(newWorkingTree)
                .setChildPath("table2/table2.1").setIndex(true).call();
        assertTrue(ref.isPresent());
    }

    @Test
    public void testImportAllWithDifferentFeatureTypesAndDestPath() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        importOp.setDestinationPath("dest");
        importOp.call();
        Iterator<NodeRef> features = cli.getGeogit().command(LsTreeOp.class)
                .setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES).call();
        ArrayList<NodeRef> list = Lists.newArrayList(features);
        assertEquals(3, list.size());
        TreeSet<ObjectId> set = Sets.newTreeSet();
        for (NodeRef node : list) {
            set.add(node.getMetadataId());
        }
        assertEquals(2, set.size());
    }

    @Test
    public void testImportAllWithDifferentFeatureTypesAndDestPathAndAdd() throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geom", Point.class);
        builder.add("label", String.class);
        builder.setName("table1");
        SimpleFeatureType type = builder.buildFeatureType();
        GeometryFactory gf = new GeometryFactory();
        SimpleFeature feature = SimpleFeatureBuilder.build(type,
                new Object[] { gf.createPoint(new Coordinate(0, 0)), "feature0" }, "feature");
        cli.getGeogit().getRepository().getWorkingTree().insert("table1", feature);
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        importOp.setOverwrite(false);
        importOp.setDestinationPath("dest");
        importOp.call();
        Iterator<NodeRef> features = cli.getGeogit().command(LsTreeOp.class)
                .setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES).call();
        ArrayList<NodeRef> list = Lists.newArrayList(features);
        assertEquals(4, list.size());
        TreeSet<ObjectId> set = Sets.newTreeSet();
        for (NodeRef node : list) {
            set.add(node.getMetadataId());
        }
        assertEquals(2, set.size());
    }

    @Test
    public void testAdd() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setTable("table1");
        importOp.call();
        importOp.setTable("table2");
        importOp.setDestinationPath("table1");
        importOp.setOverwrite(false);
        importOp.call();
        Iterator<NodeRef> features = cli.getGeogit().command(LsTreeOp.class)
                .setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES).call();
        ArrayList<NodeRef> list = Lists.newArrayList(features);
        assertEquals(3, list.size());
        TreeSet<ObjectId> set = Sets.newTreeSet();
        for (NodeRef node : list) {
            set.add(node.getMetadataId());
        }
        assertEquals(2, set.size());
    }

    @Test
    public void testAlter() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setTable("table1");
        importOp.call();
        importOp.setTable("table2");
        importOp.setDestinationPath("table1");
        importOp.setAlter(true);
        importOp.call();
        Iterator<NodeRef> features = cli.getGeogit().command(LsTreeOp.class)
                .setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES).call();
        ArrayList<NodeRef> list = Lists.newArrayList(features);
        assertEquals(3, list.size());
        Optional<RevFeature> feature = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:table1/table1.1").call(RevFeature.class);
        assertTrue(feature.isPresent());
        ImmutableList<Optional<Object>> values = feature.get().getValues();
        assertEquals(2, values.size());
        assertTrue(values.get(0).isPresent());
        assertFalse(values.get(1).isPresent());
        TreeSet<ObjectId> set = Sets.newTreeSet();
        for (NodeRef node : list) {
            set.add(node.getMetadataId());
        }
        assertEquals(1, set.size());
        Optional<RevFeatureType> featureType = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(set.iterator().next()).call(RevFeatureType.class);
        assertTrue(featureType.isPresent());
        assertEquals("table2", featureType.get().getName().getLocalPart());
    }

    @Test
    public void testDeleteException() throws Exception {
        WorkingTree workTree = mock(WorkingTree.class);
        CommandLocator cmdl = mock(CommandLocator.class);
        when(cmdl.getWorkingTree()).thenReturn(workTree);
        doThrow(new Exception("Exception")).when(workTree).delete(any(Name.class));
        ImportOp importOp = new ImportOp();
        importOp.setCommandLocator(cmdl);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    private void setUpGeogit(GeogitCLI cli) throws Exception {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        when(platform.getUserHome()).thenReturn(userhome);

        cli.setPlatform(platform);
    }
}
