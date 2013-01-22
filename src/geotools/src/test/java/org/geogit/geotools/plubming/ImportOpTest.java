package org.geogit.geotools.plubming;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.NodeRef;
import org.geogit.api.Platform;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geogit.geotools.porcelain.TestHelper;
import org.geogit.repository.WorkingTree;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.type.Name;

import com.google.common.base.Optional;

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
                .setParent(newWorkingTree).setChildPath("table1/table1.1").call();
        assertTrue(ref.isPresent());

        ref = cli.getGeogit().command(FindTreeChild.class).setParent(newWorkingTree)
                .setChildPath("table1/table1.2").call();
        assertTrue(ref.isPresent());
    }

    @Test
    public void testImportAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);

        RevTree newWorkingTree = importOp.call();
        Optional<NodeRef> ref = cli.getGeogit().command(FindTreeChild.class)
                .setParent(newWorkingTree).setChildPath("table1/table1.1").call();
        assertTrue(ref.isPresent());

        ref = cli.getGeogit().command(FindTreeChild.class).setParent(newWorkingTree)
                .setChildPath("table1/table1.2").call();
        assertTrue(ref.isPresent());

        ref = cli.getGeogit().command(FindTreeChild.class).setParent(newWorkingTree)
                .setChildPath("table2/table2.1").call();
        assertTrue(ref.isPresent());
    }

    @Test
    public void testDeleteException() throws Exception {
        WorkingTree workTree = mock(WorkingTree.class);
        doThrow(new Exception("Exception")).when(workTree).delete(any(Name.class));
        ImportOp importOp = new ImportOp();
        importOp.setWorkTree(workTree);
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
