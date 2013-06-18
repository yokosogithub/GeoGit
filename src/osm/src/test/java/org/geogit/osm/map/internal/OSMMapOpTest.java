/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.map.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.Node;
import org.geogit.api.Platform;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.in.internal.OSMImportOp;
import org.geogit.repository.WorkingTree;
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

public class OSMMapOpTest extends Assert {

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
    public void testMappingWays() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        GeoGIT geogit = cli.getGeogit();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", FieldType.LINESTRING);
        fields.put("lit", FieldType.STRING);
        MappingRule mappingRule = new MappingRule("onewaystreets", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        unstaged = workTree.countUnstaged("onewaystreets");
        assertEquals(1, unstaged);
        Optional<Node> feature = workTree.findUnstaged("onewaystreets/31045880");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(4, values.size());
        String wkt = "LINESTRING (7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals("yes", values.get(1).get());
    }

    @Test
    public void testMappingNodes() throws Exception {
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("highway", Lists.newArrayList("bus_stop"));
        fields.put("geom", FieldType.POINT);
        fields.put("name", FieldType.STRING);
        MappingRule mappingRule = new MappingRule("busstops", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        cli.getGeogit().command(OSMMapOp.class).setMapping(mapping).call();
        unstaged = workTree.countUnstaged("busstops");
        assertEquals(2, unstaged);
        Optional<Node> feature = workTree.findUnstaged("busstops/507464799");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = cli.getGeogit().command(ResolveFeatureType.class)
                .setRefSpec("WORK_HEAD:busstops/507464799").call();
        assertTrue(featureType.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(3, values.size());
        String wkt = "POINT (7.1959361 50.739397)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals(507464799l, values.get(0).get());
    }

    @Test
    public void testMappingwithNoGeometry() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        filters.put("oneway", Lists.newArrayList("yes"));
        fields.put("lit", FieldType.STRING);
        MappingRule mappingRule = new MappingRule("onewaystreets", filters, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        try {
            cli.getGeogit().command(OSMMapOp.class).setMapping(mapping).call();
            fail();
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().startsWith(
                    "The mapping rule does not define a geometry field"));
        }
    }

    @Test
    public void testMappingWithNoFilter() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        fields.put("lit", FieldType.STRING);
        fields.put("geom", FieldType.LINESTRING);
        MappingRule mappingRule = new MappingRule("allways", filters, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        cli.getGeogit().command(OSMMapOp.class).setMapping(mapping).call();
        long allways = workTree.countUnstaged("allways");
        assertTrue(allways > 0);
        long ways = workTree.countUnstaged("way");
        assertEquals(ways, allways);
    }

    @Test
    public void testMappingWithEmptyTagValueList() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        fields.put("lit", FieldType.STRING);
        fields.put("geom", FieldType.POINT);
        filters.put("highway", new ArrayList<String>());
        MappingRule mappingRule = new MappingRule("mapped", filters, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        cli.getGeogit().command(OSMMapOp.class).setMapping(mapping).call();
        long mapped = workTree.countUnstaged("mapped");
        assertEquals(4, mapped);
    }
}
