/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geogit.api.Node;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.FieldType;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class OSMMapOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMappingWays() throws Exception {
        // import and check that we have both ways and nodes
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        MappingRule mappingRule = new MappingRule("onewaystreets", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);

        // Map and check that mapping tree is created
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        unstaged = workTree.countUnstaged("onewaystreets");
        assertEquals(1, unstaged);

        // Check that mapping was correctly performed
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
        // import and check that we have nodes
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("highway", Lists.newArrayList("bus_stop"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POINT));
        fields.put("name", new AttributeDefinition("name", FieldType.STRING));
        MappingRule mappingRule = new MappingRule("busstops", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);

        // Map and check that mapping tree is created
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        unstaged = workTree.countUnstaged("busstops");
        assertEquals(2, unstaged);

        // Check that mapping was correctly performed
        Optional<Node> feature = workTree.findUnstaged("busstops/507464799");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("WORK_HEAD:busstops/507464799").call();
        assertTrue(featureType.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(3, values.size());
        String wkt = "POINT (7.1959361 50.739397)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals(507464799l, values.get(0).get());
    }

    @Test
    public void testMappingNodesWithAlias() throws Exception {
        // import and check that we have nodes
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("highway", Lists.newArrayList("bus_stop"));
        fields.put("geom", new AttributeDefinition("the_geometry", FieldType.POINT));
        fields.put("name", new AttributeDefinition("the_name", FieldType.STRING));
        MappingRule mappingRule = new MappingRule("busstops", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);

        // Map and check that mapping tree is created
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        unstaged = workTree.countUnstaged("busstops");
        assertEquals(2, unstaged);

        // Check that mapping was correctly performed
        Optional<Node> feature = workTree.findUnstaged("busstops/507464799");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("WORK_HEAD:busstops/507464799").call();
        assertTrue(featureType.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        ImmutableList<PropertyDescriptor> descriptors = featureType.get().sortedDescriptors();
        assertEquals("the_name", descriptors.get(1).getName().getLocalPart());
        assertEquals("Gielgen", values.get(1).get());
        assertEquals("the_geometry", descriptors.get(2).getName().getLocalPart());

    }

    @Test
    public void testMappingwithNoGeometry() throws Exception {
        // Test that an exception is thrown when the mapping does not contain a geometry field

        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);

        // Define a wrong mapping without geometry
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        filters.put("oneway", Lists.newArrayList("yes"));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        MappingRule mappingRule = new MappingRule("onewaystreets", filters, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);

        // Try to create a mapping
        try {
            geogit.command(OSMMapOp.class).setMapping(mapping).call();
            fail();
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().startsWith(
                    "The mapping rule does not define a geometry field"));
        }
    }

    @Test
    public void testMappingWithNoFilter() throws Exception {
        // Test that if no filter is specified in a mapping rule, all entities pass the filter

        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        MappingRule mappingRule = new MappingRule("allways", filters, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        long allways = workTree.countUnstaged("allways");
        assertTrue(allways > 0);
        long ways = workTree.countUnstaged("way");
        assertEquals(ways, allways);
    }

    @Test
    public void testMappingWithEmptyTagValueList() throws Exception {
        // Test that when no tags are specified, all entities pass the filter

        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POINT));
        filters.put("highway", new ArrayList<String>());
        MappingRule mappingRule = new MappingRule("mapped", filters, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        long mapped = workTree.countUnstaged("mapped");
        assertEquals(4, mapped);
    }

}
