/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.porcelain;

import java.io.File;
import java.util.Arrays;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.porcelain.CommitOp;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.InvalidParameterException;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.AbstractDataStoreFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ShpExportTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private GeogitCLI cli;

    private AbstractDataStoreFactory factory;

    @Before
    public void oneTimeSetup() throws Exception {
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
        String shapeFileName = new File(geogit.getPlatform().pwd(), "TestPoints.shp")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("Points", shapeFileName);
        exportCommand.dataStoreFactory = factory;
        exception.expect(CommandFailedException.class);
        exportCommand.run(cli);

        deleteShapeFile(shapeFileName);
    }

    @Test
    public void testExport() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = new File(geogit.getPlatform().pwd(), "TestPoints.shp")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("Points", shapeFileName);
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);

        deleteShapeFile(shapeFileName);
    }

    @Test
    public void testExportWithNullFeatureType() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = new File(geogit.getPlatform().pwd(), "TestPoints.shp")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList(null, shapeFileName);
        exportCommand.dataStoreFactory = factory;
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithInvalidFeatureType() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = new File(geogit.getPlatform().pwd(), "TestPoints.shp")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("invalidType", shapeFileName);
        exportCommand.dataStoreFactory = factory;
        exception.expect(InvalidParameterException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithFeatureNameInsteadOfType() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = new File(geogit.getPlatform().pwd(), "TestPoints.shp")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("Points/Points.1", shapeFileName);
        exportCommand.dataStoreFactory = factory;
        try {
            exportCommand.run(cli);
            fail();
        } catch (InvalidParameterException e) {

        } finally {
            deleteShapeFile(shapeFileName);
        }
    }

    @Test
    public void testExportToFileThatAlreadyExists() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = new File(geogit.getPlatform().pwd(), "TestPoints.shp")
                .getAbsolutePath();
        ;
        exportCommand.args = Arrays.asList("WORK_HEAD:Points", shapeFileName);
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", shapeFileName);
        try {
            exportCommand.run(cli);
            fail();
        } catch (CommandFailedException e) {

        } finally {
            deleteShapeFile(shapeFileName);
        }
    }

    @Test
    public void testExportWithNoArgs() throws Exception {
        ShpExport exportCommand = new ShpExport();
        exportCommand.args = Arrays.asList();
        exportCommand.dataStoreFactory = TestHelper.createNullTestFactory();
        exception.expect(CommandFailedException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportToFileThatAlreadyExistsWithOverwrite() throws Exception {
        ShpExport exportCommand = new ShpExport();
        String shapeFileName = new File(geogit.getPlatform().pwd(), "TestPoints.shp")
                .getAbsolutePath();
        exportCommand.args = Arrays.asList("Points", shapeFileName);
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);

        exportCommand.args = Arrays.asList("Lines", shapeFileName);
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
