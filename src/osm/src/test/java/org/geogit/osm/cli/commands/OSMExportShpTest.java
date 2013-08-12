/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.cli.commands.OSMMap;
import org.geogit.osm.internal.OSMImportOp;
import org.geogit.repository.WorkingTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OSMExportShpTest extends Assert {

    private GeogitCLI cli;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);
        File workingDirectory = tempFolder.getRoot();
        Platform platform = new TestPlatform(workingDirectory);
        cli.setPlatform(platform);
        cli.execute("init");
        cli.execute("config", "user.name", "Gabriel Roldan");
        cli.execute("config", "user.email", "groldan@opengeo.org");
        assertTrue(new File(workingDirectory, ".geogit").exists());

    }

    @Test
    public void testExportToShapefileWithMapping() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        String mappingFilename = OSMMap.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        File exportFile = new File(tempFolder.getRoot(), "export.shp");
        cli.execute("osm", "export-shp", exportFile.getAbsolutePath(), "--mapping",
                mappingFile.getAbsolutePath());
        assertTrue(exportFile.exists());
        cli.execute("shp", "import", "-d", "mapped", exportFile.getAbsolutePath());
        unstaged = workTree.countUnstaged("mapped").getCount();
        assertTrue(unstaged > 0);
    }

    @Test
    public void testExportToShapefileWithMappingWithoutGeometry() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        String mappingFilename = OSMMap.class.getResource("no_geometry_mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        File exportFile = new File(tempFolder.getRoot(), "export.shp");
        try {
            cli.execute("osm", "export-shp", exportFile.getAbsolutePath(), "--mapping",
                    mappingFile.getAbsolutePath());
            fail();
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().startsWith(
                    "The mapping rule does not define a geometry field"));
        }
    }

}