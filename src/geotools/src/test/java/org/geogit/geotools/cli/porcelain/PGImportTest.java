/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.porcelain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.AbstractDataStoreFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.exceptions.base.MockitoException;

/**
 *
 */
public class PGImportTest extends Assert {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogitCLI cli;

    private static AbstractDataStoreFactory factory;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        factory = TestHelper.createTestFactory();
    }

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        setUpGeogit(cli);
    }

    @After
    public void tearDown() throws Exception {
        cli.close();
    }

    @Test
    public void testImport() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = true;
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testNoTableNotAll() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = false;
        importCommand.table = "";
        importCommand.dataStoreFactory = factory;
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
    }

    @Test
    public void testAllAndTable() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = true;
        importCommand.table = "table1";
        importCommand.dataStoreFactory = factory;
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
    }

    @Test
    public void testImportTable() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = false;
        importCommand.table = "table1";
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testImportHelp() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.help = true;
        importCommand.run(cli);
    }

    @Test
    public void testInvalidDatabaseParams() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.commonArgs.host = "nonexistent";
        importCommand.all = true;
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
    }

    @Test
    public void testImportException() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        GeogitCLI mockCli = spy(new GeogitCLI(consoleReader));

        setUpGeogit(mockCli);

        when(mockCli.getConsole()).thenThrow(new MockitoException("Exception"));
        PGImport importCommand = new PGImport();
        importCommand.all = true;
        importCommand.dataStoreFactory = factory;
        exception.expect(MockitoException.class);
        importCommand.run(mockCli);
    }

    @Test
    public void testImportNonExistentTable() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = false;
        importCommand.table = "nonexistent";
        importCommand.dataStoreFactory = factory;
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
    }

    @Test
    public void testEmptyTable() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = true;
        importCommand.dataStoreFactory = TestHelper.createEmptyTestFactory();
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
    }

    @Test
    public void testNullDataStore() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = true;
        importCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
    }

    @Test
    public void testImportGetNamesException() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = true;
        importCommand.dataStoreFactory = TestHelper.createFactoryWithGetNamesException();
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
    }

    @Test
    public void testImportFeatureSourceException() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.all = true;
        importCommand.dataStoreFactory = TestHelper.createFactoryWithGetFeatureSourceException();
        exception.expect(CommandFailedException.class);
        importCommand.run(cli);
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
