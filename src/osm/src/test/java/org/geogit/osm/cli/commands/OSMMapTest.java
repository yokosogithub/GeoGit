/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;
import java.util.Iterator;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Node;
import org.geogit.api.Platform;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
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
import com.vividsolutions.jts.geom.Polygon;

public class OSMMapTest extends Assert {

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
    public void testMapping() throws Exception {
        // import and check
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        // map
        String mappingFilename = OSMMap.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "map", mappingFile.getAbsolutePath());
        // check it all went fine and the mapped tree is created
        unstaged = workTree.countUnstaged("onewaystreets").getCount();
        assertEquals(1, unstaged);
        Optional<Node> feature = workTree.findUnstaged("onewaystreets/31045880");
        assertTrue(feature.isPresent());
        // check that a feature was correctly mapped
        Optional<RevFeature> revFeature = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        String wkt = "LINESTRING (7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals("345117525;345117526;1300224327;345117527", values.get(3).get());
        assertEquals("yes", values.get(1).get());
    }

    @Test
    public void testMappingWithWrongMappingFile() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        String mappingFilename = OSMMap.class.getResource("wrong_mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        try {
            cli.execute("osm", "map", mappingFile.getAbsolutePath());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Error parsing mapping definition"));
        }
    }

    @Test
    public void testMappingWithMissingFile() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        try {
            cli.execute("osm", "map", "awrongpath/awroongfile.json");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("The specified mapping file does not exist"));
        }
    }

    @Test
    public void testMappingWithNoFilter() throws Exception {
        // check that if no filter is passed, all entities are mapped
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        String mappingFilename = OSMMap.class.getResource("no_filter_mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "map", mappingFile.getAbsolutePath());
        long allways = workTree.countUnstaged("all_ways");
        assertTrue(allways > 0);
        long ways = workTree.countUnstaged("way");
        assertEquals(ways, allways);
    }

    @Test
    public void testMappingWithPolygons() throws Exception {
        // test a mapping with a a mapping rule that uses the polygon geometry type
        String filename = OSMImportOp.class.getResource("closed_ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        String mappingFilename = OSMMap.class.getResource("polygons_mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        cli.execute("osm", "map", mappingFile.getAbsolutePath());
        Iterator<DiffEntry> iter = workTree.getUnstaged("areas");
        assertTrue(iter.hasNext());
        Optional<RevFeatureType> ft = cli.getGeogit().command(ResolveFeatureType.class)
                .setRefSpec("WORK_HEAD:" + iter.next().newPath()).call();
        assertTrue(ft.isPresent());
        assertEquals(Polygon.class, ft.get().sortedDescriptors().get(1).getType().getBinding());

    }

}
