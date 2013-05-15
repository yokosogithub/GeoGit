/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.in.cli;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Node;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.LogOp;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.base.OSMLogEntry;
import org.geogit.osm.base.ReadOSMLogEntries;
import org.geogit.osm.in.internal.OSMImportOp;
import org.geogit.osm.map.cli.OSMMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class OSMDownloadTest extends Assert {

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

    @Ignore
    @Test
    public void testDownloadNodes() throws Exception {
        String filename = OSMImportOp.class.getResource("nodes_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "download", "-f", filterFile.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        Iterator<RevCommit> log = cli.getGeogit().command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownloadEmptyFilter() throws Exception {
        String filename = OSMImportOp.class.getResource("empty_filter.txt").getFile();
        File filterFile = new File(filename);
        try {
            cli.execute("osm", "download", "-f", filterFile.getAbsolutePath());
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    @Ignore
    @Test
    public void testDowloadNodesWithDestinationFile() throws Exception {
        String filename = OSMImportOp.class.getResource("nodes_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        File downloadFile = File.createTempFile("osm-geogit", ".xml");
        cli.execute("osm", "download", "-f", filterFile.getAbsolutePath(), "--saveto",
                downloadFile.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        Iterator<RevCommit> log = cli.getGeogit().command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownloadNodesWithRelativeDestinationFile() throws Exception {
        String filename = OSMImportOp.class.getResource("nodes_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "download", "-f", filterFile.getAbsolutePath(), "--saveto",
                "./osm-geogit.xml");
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        Iterator<RevCommit> log = cli.getGeogit().command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownaloadWays() throws Exception {
        String filename = OSMImportOp.class.getResource("ways_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "download", "-f", filterFile.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        Iterator<RevCommit> log = cli.getGeogit().command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownloadWaysWithoutNodes() throws Exception {
        String filename = OSMImportOp.class.getResource("ways_no_nodes_overpass_filter.txt")
                .getFile();
        File filterFile = new File(filename);
        try {
            cli.execute("osm", "download", "-f", filterFile.getAbsolutePath());
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith(
                    "Some elements returned by the specified filter could not be processed"));
        }

    }

    @Ignore
    @Test
    public void testDownloadWithBBox() throws Exception {
        cli.execute("osm", "download", "-b", "50.79", "7.19", "50.8", "7.20");
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        Iterator<RevCommit> log = cli.getGeogit().command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownloadWithBBoxAndAlternativeUrl() throws Exception {
        String url = "http://api.openstreetmap.fr/oapi/interpreter/";
        cli.execute("osm", "download", url, "-b", "50.79", "7.19", "50.8", "7.20");
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
    }

    @Ignore
    @Test
    public void testDownloadWithBBoxAndMapping() throws Exception {
        String mappingFilename = OSMMap.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "download", "--mapping", mappingFile.getAbsolutePath(), "-b", "50.79",
                "7.19", "50.8", "7.20");
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        tree = cli.getGeogit().getRepository().getRootTreeChild("onewaystreets");
        assertTrue(tree.isPresent());
    }

    @Test
    public void testImportWithWrongBBox() throws Exception {
        try {
            cli.execute("osm", "download", "-b", "asdads", "7.19", "50.8", "7.20");
            fail();
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testImportWithWrongUrl() throws Exception {
        try {
            cli.execute("osm", "download", "wrongurl", "-b", "50.79", "7.19", "50.8", "7.20");
            fail();
        } catch (IllegalStateException e) {
        }
    }

    @Ignore
    @Test
    public void testUpdate() throws Exception {
        String filename = OSMImportOp.class.getResource("fire_station_filter.txt").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "download", "-f", filterFile.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        cli.execute("osm", "download", "--update");
    }

}
