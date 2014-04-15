/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.osm.internal.MappingRule.DefaultField;
import org.geogit.osm.internal.log.ResolveOSMMappingLogFolder;
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
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filter = Maps.newHashMap();
        filter.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("onewaystreets", filter, filterExclude, fields,
                null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:onewaystreets/31045880").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(4, values.size());
        String wkt = "LINESTRING (7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals("yes", values.get(1).get());

        // Check that the corresponding log files have been added
        File osmMapFolder = geogit.command(ResolveOSMMappingLogFolder.class).call();
        file = new File(osmMapFolder, "onewaystreets");
        assertTrue(file.exists());
        file = new File(osmMapFolder, geogit.getRepository().getWorkingTree().getTree().getId()
                .toString());
        assertTrue(file.exists());

    }

    @Test
    public void testMappingDefaultFields() throws Exception {
        // import and check that we have both ways and nodes
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filter = Maps.newHashMap();
        filter.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        ArrayList<DefaultField> defaultFields = Lists.newArrayList();
        defaultFields.add(DefaultField.timestamp);
        defaultFields.add(DefaultField.visible);
        MappingRule mappingRule = new MappingRule("onewaystreets", filter, null, fields,
                defaultFields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:onewaystreets/31045880").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(6, values.size());
        String wkt = "LINESTRING (7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781)";
        assertEquals(wkt, values.get(4).get().toString());
        assertEquals("yes", values.get(3).get());
        assertEquals(true, values.get(2).get());
        assertEquals(1318750940000L, values.get(1).get());
        Optional<RevFeatureType> revFeatureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("HEAD:onewaystreets/31045880").call();
        assertTrue(revFeatureType.isPresent());
        ImmutableList<PropertyDescriptor> descriptors = revFeatureType.get().sortedDescriptors();
        assertEquals("timestamp", descriptors.get(1).getName().toString());
        assertEquals("visible", descriptors.get(2).getName().toString());

    }

    @Test
    public void testMappingOnlyClosedPolygons() throws Exception {
        // import and check that we have both ways and nodes
        String filename = OSMImportOp.class.getResource("ways_restriction.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filter = Maps.newHashMap();
        filter.put("geom", Lists.newArrayList("closed"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POLYGON));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("polygons", filter, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:polygons/31045880").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(4, values.size());
        String wkt = "POLYGON ((7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781, 7.1923367 50.7395887))";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals("yes", values.get(1).get());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("HEAD:polygons/24777894")
                .call(RevFeature.class);
        assertFalse(revFeature.isPresent());

    }

    @Test
    public void testExcludePoligonsWithLessThan3Points() throws Exception {
        // import and check that we have both ways and nodes
        String filename = OSMImportOp.class.getResource("ways_restriction.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filter = Maps.newHashMap();
        fields.put("geom", new AttributeDefinition("geom", FieldType.POLYGON));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("polygons", filter, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:polygons/31045880").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("HEAD:polygons/24777894")
                .call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("HEAD:polygons/51502277")
                .call(RevFeature.class);
        assertFalse(revFeature.isPresent());

    }

    @Test
    public void testMappingOnlyOpenLines() throws Exception {
        // import and check that we have both ways and nodes
        String filename = OSMImportOp.class.getResource("ways_restriction.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filter = Maps.newHashMap();
        filter.put("geom", Lists.newArrayList("open"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("nonclosed", filter, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:nonclosed/31045880").call(RevFeature.class);
        assertFalse(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("HEAD:nonclosed/24777894")
                .call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("HEAD:nonclosed/51502277")
                .call(RevFeature.class);
        assertTrue(revFeature.isPresent());

    }

    @Test
    public void testMappingNodes() throws Exception {
        // import and check that we have nodes
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();
        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("highway", Lists.newArrayList("bus_stop"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POINT));
        fields.put("name", new AttributeDefinition("name", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("busstops", mappings, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:busstops/507464799").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("HEAD:busstops/507464799").call();
        assertTrue(featureType.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(3, values.size());
        String wkt = "POINT (7.1959361 50.739397)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals(507464799l, values.get(0).get());

        // Check that the corresponding log files have been added
        File osmMapFolder = geogit.command(ResolveOSMMappingLogFolder.class).call();
        file = new File(osmMapFolder, "busstops");
        assertTrue(file.exists());
        file = new File(osmMapFolder, geogit.getRepository().getWorkingTree().getTree().getId()
                .toString());
        assertTrue(file.exists());
    }

    @Test
    public void testMappingWithExclusion() throws Exception {
        // import and check that we have nodes
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();
        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filter = Maps.newHashMap();
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        filter.put("highway", Lists.newArrayList("bus_stop"));
        filterExclude.put("public_transport", Lists.newArrayList("stop_position"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POINT));
        fields.put("name", new AttributeDefinition("name", FieldType.STRING));
        fields.put("name", new AttributeDefinition("name", FieldType.STRING));
        MappingRule mappingRule = new MappingRule("busstops", filter, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:busstops/507464799").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("HEAD:busstops/507464799").call();
        assertTrue(featureType.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(3, values.size());
        String wkt = "POINT (7.1959361 50.739397)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals(507464799l, values.get(0).get());

        // Check that the excluded feature is missing
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("HEAD:busstops/507464865")
                .call(RevFeature.class);
        assertFalse(revFeature.isPresent());
    }

    @Test
    public void testMappingNodesWithAlias() throws Exception {
        // import and check that we have nodes
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();

        // Define mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("highway", Lists.newArrayList("bus_stop"));
        fields.put("geom", new AttributeDefinition("the_geometry", FieldType.POINT));
        fields.put("name", new AttributeDefinition("the_name", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("busstops", mappings, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        // Check that mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:busstops/507464799").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("HEAD:busstops/507464799").call();
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
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();
        // Define a wrong mapping without geometry
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        filters.put("oneway", Lists.newArrayList("yes"));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("onewaystreets", filters, filterExclude, fields,
                null);
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
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("allways", filters, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        Iterator<NodeRef> allways = geogit.command(LsTreeOp.class).setReference("HEAD:allways")
                .call();
        assertTrue(allways.hasNext());
        Iterator<NodeRef> ways = geogit.command(LsTreeOp.class).setReference("HEAD:allways").call();
        ArrayList<NodeRef> listWays = Lists.newArrayList(ways);
        ArrayList<NodeRef> listAllways = Lists.newArrayList(allways);
        assertEquals(listWays.size(), listAllways.size());
    }

    @Test
    public void testMappingWithEmptyTagValueList() throws Exception {
        // Test that when no tags are specified, all entities pass the filter

        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POINT));
        filters.put("highway", new ArrayList<String>());
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("mapped", filters, filterExclude, fields, null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).setReference("HEAD:mapped").call();
        ArrayList<NodeRef> list = Lists.newArrayList(iter);
        assertEquals(4, list.size());
    }

}
