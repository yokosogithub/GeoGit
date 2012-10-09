/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.pg.cli;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.AbstractDataStoreFactory;
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
public class PGDescribeTest extends Assert {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogitCLI cli;

    private static AbstractDataStoreFactory factory;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        factory = PGTestHelper.createTestFactory();
    }

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        setUpGeogit(cli);
    }

    @Test
    public void testDescribe() throws Exception {
        PGDescribe describeCommand = new PGDescribe();
        describeCommand.args.table = "table1";
        describeCommand.dataStoreFactory = factory;
        describeCommand.run(cli);
    }

    @Test
    public void testNoTable() throws Exception {
        PGDescribe describeCommand = new PGDescribe();
        describeCommand.args.table = "";
        describeCommand.dataStoreFactory = factory;
        exception.expect(Exception.class);
        describeCommand.run(cli);
    }

    @Test
    public void testNoRepository() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        PGDescribe describeCommand = new PGDescribe();
        describeCommand.args.table = "table1";
        describeCommand.dataStoreFactory = factory;
        exception.expect(IllegalStateException.class);
        describeCommand.run(cli);
    }

    @Test
    public void testDescribeException() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        GeogitCLI mockCli = spy(new GeogitCLI(consoleReader));

        setUpGeogit(mockCli);

        when(mockCli.getConsole()).thenThrow(new MockitoException("Exception"));
        PGDescribe describeCommand = new PGDescribe();
        describeCommand.args.table = "table1";
        describeCommand.dataStoreFactory = factory;
        exception.expect(MockitoException.class);
        describeCommand.run(mockCli);
    }

    @Test
    public void testFlushException() throws Exception {
        ConsoleReader consoleReader = spy(new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal()));
        GeogitCLI testCli = new GeogitCLI(consoleReader);

        setUpGeogit(testCli);

        doThrow(new IOException("Exception")).when(consoleReader).flush();

        PGDescribe describeCommand = new PGDescribe();
        describeCommand.args.table = "table1";
        describeCommand.dataStoreFactory = factory;
        exception.expect(Exception.class);
        describeCommand.run(testCli);
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
