/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.Node;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.LogOp;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.osm.internal.log.OSMLogEntry;
import org.geogit.osm.internal.log.ReadOSMLogEntries;
import org.geogit.osm.internal.log.ResolveOSMMappingLogFolder;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class OSMDownloadOpTest extends RepositoryTestCase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Ignore
    @Test
    public void testDownloadNodes() throws Exception {
        String filename = OSMImportOp.class.getResource("nodes_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setFilterFile(filterFile).setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = geogit.command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        Iterator<RevCommit> log = geogit.command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownloadEmptyFilter() throws Exception {
        String filename = OSMImportOp.class.getResource("empty_filter.txt").getFile();
        File filterFile = new File(filename);
        try {
            OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
            download.setFilterFile(filterFile).setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
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
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setFilterFile(filterFile).setSaveFile(downloadFile)
                .setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = geogit.command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        Iterator<RevCommit> log = geogit.command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownaloadWays() throws Exception {
        String filename = OSMImportOp.class.getResource("ways_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setFilterFile(filterFile).setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = geogit.getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        Iterator<RevCommit> log = geogit.command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownloadWaysWithoutNodes() throws Exception {
        String filename = OSMImportOp.class.getResource("ways_no_nodes_overpass_filter.txt")
                .getFile();
        File filterFile = new File(filename);
        try {
            OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
            download.setFilterFile(filterFile).setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("The specified filter did not return any element"));
        }

    }

    @Ignore
    @Test
    public void testDownloadWithBBox() throws Exception {
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setBbox(Arrays.asList("50.79", "7.19", "50.8", "7.20"))
                .setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        Iterator<RevCommit> log = geogit.command(LogOp.class).call();
        assertTrue(log.hasNext());
    }

    @Ignore
    @Test
    public void testDownloadWithBBoxAndAlternativeUrl() throws Exception {
        String url = "http://api.openstreetmap.fr/oapi/interpreter/";
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setBbox(Arrays.asList("50.79", "7.19", "50.8", "7.20")).setOsmAPIUrl(url).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
    }

    // @Ignore
    @Test
    public void testDownloadWithBBoxAndMapping() throws Exception {
        String mappingFilename = OSMMapOp.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setMappingFile(mappingFile)
                .setBbox(Arrays.asList("50.79", "7.19", "50.8", "7.20"))
                .setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        tree = geogit.getRepository().getRootTreeChild("onewaystreets");
        assertTrue(tree.isPresent());
        // check it has created mapping log files
        File osmMapFolder = geogit.command(ResolveOSMMappingLogFolder.class).call();
        File file = new File(osmMapFolder, "onewaystreets");
        assertTrue(file.exists());
        file = new File(osmMapFolder, geogit.getRepository().getWorkingTree().getTree().getId()
                .toString());
        assertTrue(file.exists());
    }

    @Test
    public void testImportWithWrongBBox() throws Exception {
        try {
            OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
            download.setBbox(Arrays.asList("asdads", "7.19", "50.8", "7.20"))
                    .setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
            fail();
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testImportWithWrongUrl() throws Exception {
        try {
            OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
            download.setBbox(Arrays.asList("50.79", "7.19", "50.8", "7.20"))
                    .setOsmAPIUrl("http://wrongurl.com").call();
            fail();
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Did you try to use a standard OSM server instead?"));
        }
    }

    @Ignore
    @Test
    public void testUpdate() throws Exception {
        String filename = OSMImportOp.class.getResource("fire_station_filter.txt").getFile();
        File filterFile = new File(filename);
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setFilterFile(filterFile).setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = geogit.getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = geogit.command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        OSMUpdateOp update = geogit.command(OSMUpdateOp.class);
        try {
            update.setAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        } catch (NothingToCommitException e) {
            // No new data
        }
    }

    @Ignore
    @Test
    public void testUpdatewithBBox() throws Exception {
        OSMDownloadOp download = geogit.command(OSMDownloadOp.class);
        download.setBbox(Arrays.asList("50.79", "7.19", "50.8", "7.20"))
                .setOsmAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        Optional<Node> tree = geogit.getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = geogit.getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = geogit.command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
        OSMUpdateOp update = geogit.command(OSMUpdateOp.class);
        try {
            update.setAPIUrl(OSMUtils.DEFAULT_API_ENDPOINT).call();
        } catch (NothingToCommitException e) {
            // No new data
        }
    }

}
