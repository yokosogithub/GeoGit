/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.util.Arrays;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.porcelain.CommitOp;
import org.geogit.cli.GeogitCLI;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.AbstractDataStoreFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 */
public class SLExportTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private GeogitCLI cli;

    private static AbstractDataStoreFactory factory;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        factory = TestHelper.createTestFactory();
    }

    @Override
    public void setUpInternal() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        cli.setGeogit(geogit);

        // Add points
        insertAndAdd(points1);
        insertAndAdd(points2);
        insertAndAdd(points3);

        geogit.command(CommitOp.class).call();

        // Add lines
        insertAndAdd(lines1);
        insertAndAdd(lines2);
        insertAndAdd(lines3);

        geogit.command(CommitOp.class).call();
    }

    @Override
    public void tearDownInternal() throws Exception {
        cli.close();
    }

    @Test
    public void testExport() throws Exception {

        SLExport exportCommand = new SLExport();
        exportCommand.args = Arrays.asList("Points", "Points");
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);
    }

    @Test
    public void testNullDataStore() throws Exception {
        SLExport exportCommand = new SLExport();
        exportCommand.args = Arrays.asList("Points", "Points");
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);
    }

    @Test
    public void testNoArgs() throws Exception {
        SLExport exportCommand = new SLExport();
        exportCommand.args = Arrays.asList();
        exportCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        exportCommand.run(cli);
    }

    @Test
    public void testExportToTableThatExists() throws Exception {
        SLExport exportCommand = new SLExport();
        exportCommand.args = Arrays.asList("Points", "table1");
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);
    }
}
