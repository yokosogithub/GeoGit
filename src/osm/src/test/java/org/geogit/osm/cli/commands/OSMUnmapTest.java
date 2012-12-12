/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.api.RevFeature;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.internal.OSMImportOp;
import org.geogit.repository.WorkingTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class OSMUnmapTest extends Assert {

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
        // import with mapping
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        String mappingFilename = OSMMap.class.getResource("nodes_mapping_with_aliases.json")
                .getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "import", file.getAbsolutePath(), "--mapping",
                mappingFile.getAbsolutePath(), "--no-raw");
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("busstops").getCount();
        assertTrue(unstaged > 0);
        Optional<RevFeature> revFeature = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("HEAD:busstops/507464799").call(RevFeature.class);
        assertTrue(revFeature.isPresent());

    }

    @Test
    public void testUnMappingUsingMappingFile() throws Exception {
        String mappingFilename = OSMUnmap.class.getResource("nodes_mapping_with_aliases.json")
                .getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "unmap", "busstops", "--mapping", mappingFile.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);

        Optional<RevFeature> unmapped = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/507464799").call(RevFeature.class);
        assertTrue(unmapped.isPresent());
        ImmutableList<Optional<Object>> values = unmapped.get().getValues();
        assertEquals("POINT (7.1959361 50.739397)", values.get(6).get().toString());
        assertEquals("name:Gielgen", values.get(3).get().toString());

    }

    @Test
    public void testUnMapping() throws Exception {
        cli.execute("osm", "unmap", "busstops");
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);

        Optional<RevFeature> unmapped = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/507464799").call(RevFeature.class);
        assertTrue(unmapped.isPresent());
        ImmutableList<Optional<Object>> values = unmapped.get().getValues();
        assertEquals("POINT (7.1959361 50.739397)", values.get(6).get().toString());
        assertEquals("name_alias:Gielgen", values.get(3).get().toString());

    }

}
