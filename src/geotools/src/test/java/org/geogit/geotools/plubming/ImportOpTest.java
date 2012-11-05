package org.geogit.geotools.plubming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.namespace.QName;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geogit.geotools.porcelain.TestHelper;
import org.geogit.repository.WorkingTree;
import org.geotools.data.memory.MemoryDataStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

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

    @Test
    public void testAccessorsAndMutators() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);

        MemoryDataStore testDataStore = new MemoryDataStore();
        importOp.setDataStore(testDataStore);
        assertEquals(testDataStore, importOp.getDataStore());

        importOp.setTable("table1");
        assertEquals("table1", importOp.getTable());

        importOp.setAll(true);
        assertTrue(importOp.getAll());

        importOp.setAll(false);
        assertFalse(importOp.getAll());

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
        importOp.call();
    }

    @Test
    public void testImportAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        importOp.call();
    }

    @Test
    public void testDeleteException() throws Exception {
        WorkingTree workTree = mock(WorkingTree.class);
        doThrow(new Exception("Exception")).when(workTree).delete(any(QName.class));
        ImportOp importOp = new ImportOp(workTree);
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
