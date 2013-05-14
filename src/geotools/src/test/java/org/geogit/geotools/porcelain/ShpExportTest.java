/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.io.File;
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

public class ShpExportTest extends RepositoryTestCase {

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
    public void testExportWithDifferentFeatureTypes() throws Exception {
        insertAndAdd(points1B);
        geogit.command(CommitOp.class).call();
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = "TestPoints";
        exportCommand.args = Arrays.asList("Points", shapeFileName + ".shp");
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);

        deleteShapeFile(shapeFileName);
    }

    @Test
    public void testExport() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = "TestPoints";
        exportCommand.args = Arrays.asList("Points", shapeFileName + ".shp");
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);

        deleteShapeFile(shapeFileName);
    }

    @Test
    public void testExportWithNullFeatureType() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = "TestPoints";
        exportCommand.args = Arrays.asList(null, shapeFileName + ".shp");
        exportCommand.dataStoreFactory = factory;
        exception.expect(IllegalArgumentException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithInvalidFeatureType() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = "TestPoints";
        exportCommand.args = Arrays.asList("invalidType", shapeFileName + ".shp");
        exportCommand.dataStoreFactory = factory;
        exception.expect(IllegalArgumentException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithFeatureNameInsteadOfType() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = "TestPoints";
        exportCommand.args = Arrays.asList("Points/Points.1", shapeFileName + ".shp");
        exportCommand.dataStoreFactory = factory;
        exception.expect(IllegalArgumentException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportToFileThatAlreadyExists() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = "TestPoints";
        exportCommand.args = Arrays.asList("WORK_HEAD:Points", shapeFileName + ".shp");
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", shapeFileName + ".shp");
        exportCommand.overwrite = true;
        exportCommand.run(cli);

        deleteShapeFile(shapeFileName);
    }

    @Test
    public void testExportWithNoArgs() throws Exception {
        ShpExport exportCommand = new ShpExport();
        exportCommand.args = Arrays.asList();
        exportCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        exportCommand.run(cli);
    }

    @Test
    public void testExportToFileThatAlreadyExistsWithOverwrite() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = "TestPoints";
        exportCommand.args = Arrays.asList("Points", shapeFileName + ".shp");
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", shapeFileName + ".shp");
        exportCommand.overwrite = true;
        exportCommand.run(cli);

        deleteShapeFile(shapeFileName);
    }

    private void deleteShapeFile(String shapeFileName) {
        File file = new File(shapeFileName + ".shp");
        if (file.exists()) {
            file.delete();
        }
        file = new File(shapeFileName + ".fix");
        if (file.exists()) {
            file.delete();
        }
        file = new File(shapeFileName + ".shx");
        if (file.exists()) {
            file.delete();
        }
        file = new File(shapeFileName + ".qix");
        if (file.exists()) {
            file.delete();
        }
        file = new File(shapeFileName + ".prj");
        if (file.exists()) {
            file.delete();
        }
        file = new File(shapeFileName + ".dbf");
        if (file.exists()) {
            file.delete();
        }
    }

}
