/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.in.internal;

import java.io.File;
import java.util.List;
import java.util.Map;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Node;
import org.geogit.api.Platform;
import org.geogit.api.RevFeature;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.map.internal.Mapping;
import org.geogit.osm.map.internal.MappingRule;
import org.geogit.storage.FieldType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class OSMImportOpTest extends Assert {

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
        String filename = getClass().getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.getGeogit().command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        long unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("node");
        assertTrue(unstaged > 0);
        unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("way");
        assertTrue(unstaged > 0);
    }

    @Test
    public void testImportAdd() throws Exception {
        String filename = getClass().getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.getGeogit().command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        filename = getClass().getResource("nodes.xml").getFile();
        file = new File(filename);
        cli.getGeogit().command(OSMImportOp.class).setDataSource(file.getAbsolutePath())
                .setAdd(true).call();
        long unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("node");
        assertEquals(30, unstaged);
        unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("way");
        assertEquals(4, unstaged);
    }

    @Test
    public void testImportWithMapping() throws Exception {
        String filename = getClass().getResource("ways.xml").getFile();
        File file = new File(filename);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", FieldType.LINESTRING);
        fields.put("lit", FieldType.STRING);
        MappingRule mappingRule = new MappingRule("onewaystreets", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        cli.getGeogit().command(OSMImportOp.class).setDataSource(file.getAbsolutePath())
                .setMapping(mapping).call();
        long unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("node");
        assertTrue(unstaged > 0);
        unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("way");
        assertTrue(unstaged > 0);
        unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("onewaystreets");
        assertEquals(1, unstaged);
        Optional<Node> feature = cli.getGeogit().getRepository().getWorkingTree()
                .findUnstaged("onewaystreets/31045880");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        String wkt = "LINESTRING (7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals("31045880", values.get(0).get().toString());
        assertEquals("yes", values.get(1).get());
    }

    @Test
    public void testImportWithMappingAndNoRaw() throws Exception {
        String filename = getClass().getResource("ways.xml").getFile();
        File file = new File(filename);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", FieldType.LINESTRING);
        fields.put("lit", FieldType.STRING);
        MappingRule mappingRule = new MappingRule("onewaystreets", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        cli.getGeogit().command(OSMImportOp.class).setDataSource(file.getAbsolutePath())
                .setMapping(mapping).setNoRaw(true).call();
        long unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("node");
        assertEquals(0, unstaged);
        unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("way");
        assertEquals(0, unstaged);
        unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("onewaystreets");
        assertEquals(1, unstaged);
        Optional<Node> feature = cli.getGeogit().getRepository().getWorkingTree()
                .findUnstaged("onewaystreets/31045880");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        String wkt = "LINESTRING (7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals("31045880", values.get(0).get().toString());
        assertEquals("yes", values.get(1).get());
    }

    // @Test
    // public void testImportClosedWays() throws Exception {
    // String filename = getClass().getResource("closed_ways.xml").getFile();
    // File file = new File(filename);
    // cli.getGeogit().command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
    // long unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("node");
    // assertTrue(unstaged > 0);
    // unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("way");
    // assertTrue(unstaged > 0);
    // cli.execute("add");
    // cli.execute("commit", "-m", "commit1");
    // Optional<RevFeatureType> type = cli.getGeogit().command(ResolveFeatureType.class)
    // .setRefSpec("way/24777894").call();
    // assertTrue(type.isPresent());
    // assertEquals(OSMUtils.closedWayType(), type.get().type());
    // }

}
