/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.api.RevFeatureType;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.internal.OSMImportOp;
import org.geogit.repository.WorkingTree;
import org.geogit.osm.internal.log.ResolveOSMMappingLogFolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class OSMImportTest extends Assert {

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
    public void testImport() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workingTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workingTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        unstaged = workingTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
    }

    @Test
    public void testImportWithMapping() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        String mappingFilename = OSMMap.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "import", file.getAbsolutePath(), "--mapping",
                mappingFile.getAbsolutePath());
                .countUnstaged("onewaystreets").getCount();
        Optional<RevFeatureType> revFeatureType = cli.getGeogit().command(ResolveFeatureType.class)
                .setRefSpec("onewaystreets").call();
        assertTrue(revFeatureType.isPresent());
        // check it has created mapping log files
        File osmMapFolder = cli.getGeogit().command(ResolveOSMMappingLogFolder.class).call();
        file = new File(osmMapFolder, "onewaystreets");
        assertTrue(file.exists());
        file = new File(osmMapFolder, cli.getGeogit().getRepository().getWorkingTree().getTree()
                .getId().toString());
        assertTrue(file.exists());
    }

    @Test
    public void testImportWithMapingAndNoRaw() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        String mappingFilename = OSMMap.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "import", file.getAbsolutePath(), "--mapping",
                mappingFile.getAbsolutePath(), "--no-raw");
        Optional<RevFeatureType> revFeatureType = cli.getGeogit().command(ResolveFeatureType.class)
                .setRefSpec("onewaystreets").call();
        assertTrue(revFeatureType.isPresent());
        revFeatureType = cli.getGeogit().command(ResolveFeatureType.class).setRefSpec("way").call();
        assertFalse(revFeatureType.isPresent());
        revFeatureType = cli.getGeogit().command(ResolveFeatureType.class).setRefSpec("node")
                .call();
        assertFalse(revFeatureType.isPresent());
        // check it has not created mapping log files
        File osmMapFolder = cli.getGeogit().command(ResolveOSMMappingLogFolder.class).call();
        file = new File(osmMapFolder, "onewaystreets");
        assertFalse(file.exists());
        file = new File(osmMapFolder, cli.getGeogit().getRepository().getWorkingTree().getTree()
                .getId().toString());
        assertFalse(file.exists());
    }

}
