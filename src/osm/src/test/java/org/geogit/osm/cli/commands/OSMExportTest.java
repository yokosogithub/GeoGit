/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;
import java.util.Iterator;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevTree;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.test.functional.CLITestInjectorBuilder;
import org.geogit.osm.internal.OSMImportOp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class OSMExportTest extends Assert {

    private GeogitCLI cli;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);
        File workingDirectory = tempFolder.getRoot();
        TestPlatform platform = new TestPlatform(workingDirectory);
        GlobalInjectorBuilder.builder = new CLITestInjectorBuilder(platform);
        cli.setPlatform(platform);
        cli.execute("init");
        cli.execute("config", "user.name", "Gabriel Roldan");
        cli.execute("config", "user.email", "groldan@opengeo.org");
        assertTrue(new File(workingDirectory, ".geogit").exists());

    }

    @Test
    public void testExportOnlyNodes() throws Exception {
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "message");
        Optional<RevTree> tree = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("HEAD:node").call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        File exportFile = new File(tempFolder.getRoot(), "export.xml");
        cli.execute("osm", "export", exportFile.getAbsolutePath());
    }

    @Test
    public void testExportAndThenReimport() throws Exception {
        String filename = OSMImportOp.class.getResource("fire.xml").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "import", filterFile.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "message");
        Optional<ObjectId> id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:node")
                .call();
        assertTrue(id.isPresent());
        id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:way").call();
        assertTrue(id.isPresent());
        File file = new File(tempFolder.getRoot(), "export.xml");
        cli.execute("osm", "export", file.getAbsolutePath());
        cli.execute("rm", "-r", "node");
        cli.execute("rm", "-r", "way");
        cli.execute("add");
        cli.execute("commit", "-m", "Deleted OSM data");
        id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:node").call();
        assertFalse(id.isPresent());
        id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:way").call();
        assertFalse(id.isPresent());
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "reimport");
        Optional<RevTree> tree = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("HEAD:node").call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        tree = cli.getGeogit().command(RevObjectParse.class).setRefSpec("HEAD:way")
                .call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        Iterator<DiffEntry> diffs = cli.getGeogit().command(DiffOp.class).setNewVersion("HEAD")
                .setOldVersion("HEAD~2").call();
        assertFalse(diffs.hasNext());
    }

    @Test
    public void testExportFromWorkingHead() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "message");
        Optional<RevTree> tree = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("HEAD:node").call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        tree = cli.getGeogit().command(RevObjectParse.class).setRefSpec("HEAD:way")
                .call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        File exportFile = new File(tempFolder.getRoot(), "export.xml");
        cli.execute("osm", "export", exportFile.getAbsolutePath(), "WORK_HEAD");
        cli.execute("rm", "-r", "node");
        cli.execute("rm", "-r", "way");
        tree = cli.getGeogit().command(RevObjectParse.class).setRefSpec("WORK_HEAD:node")
                .call(RevTree.class);
        assertFalse(tree.isPresent());
        tree = cli.getGeogit().command(RevObjectParse.class).setRefSpec("WORK_HEAD:way")
                .call(RevTree.class);
        assertFalse(tree.isPresent());
        cli.execute("osm", "import", exportFile.getAbsolutePath());
        tree = cli.getGeogit().command(RevObjectParse.class).setRefSpec("HEAD:node")
                .call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        tree = cli.getGeogit().command(RevObjectParse.class).setRefSpec("HEAD:way")
                .call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
    }

    @Test
    public void testExportAndThenReimportUsingPbfFormat() throws Exception {
        String filename = OSMImportOp.class.getResource("fire.xml").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "import", filterFile.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "message");
        Optional<ObjectId> id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:node")
                .call();
        assertTrue(id.isPresent());
        id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:way").call();
        assertTrue(id.isPresent());
        File file = new File(tempFolder.getRoot(), "export.pbf");
        cli.execute("osm", "export", file.getAbsolutePath());
        cli.execute("rm", "-r", "node");
        cli.execute("rm", "-r", "way");
        cli.execute("add");
        cli.execute("commit", "-m", "Deleted OSM data");
        id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:node").call();
        assertFalse(id.isPresent());
        id = cli.getGeogit().command(RevParse.class).setRefSpec("HEAD:way").call();
        assertFalse(id.isPresent());
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "reimport");
        Optional<RevTree> tree = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("HEAD:node").call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        tree = cli.getGeogit().command(RevObjectParse.class).setRefSpec("HEAD:way")
                .call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
    }

}
