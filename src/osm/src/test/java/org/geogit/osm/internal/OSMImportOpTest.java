/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.geogit.api.Node;
import org.geogit.api.RevFeature;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.osm.internal.log.ResolveOSMMappingLogFolder;
import org.geogit.storage.FieldType;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class OSMImportOpTest extends RepositoryTestCase {
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
    public void testImport() throws Exception {
        String filename = getClass().getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        long unstaged = geogit.getRepository().getWorkingTree().countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        unstaged = geogit.getRepository().getWorkingTree().countUnstaged("way").getCount();
        assertTrue(unstaged > 0);
    }

    @Test
    public void testImportAdd() throws Exception {
        // import two files, using the add option, so the nodes imported by the first one are not
        // removed when adding those from the second one
        String filename = getClass().getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        filename = getClass().getResource("nodes.xml").getFile();
        file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).setAdd(true).call();
        // Check that the working tree contains elements from both imports
        long unstaged = geogit.getRepository().getWorkingTree().countUnstaged("node").getCount();
        assertEquals(30, unstaged);
        unstaged = geogit.getRepository().getWorkingTree().countUnstaged("way").getCount();
        assertEquals(4, unstaged);
    }

    @Test
    public void testImportWithMapping() throws Exception {
        String filename = getClass().getResource("ways.xml").getFile();
        File file = new File(filename);

        // Define a mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("onewaystreets", mappings, filterExclude, fields,
                null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);

        // import with mapping and check import went ok
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).setMapping(mapping)
                .call();
        Optional<RevTree> tree = geogit.command(RevObjectParse.class).setRefSpec("HEAD:node")
                .call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        tree = geogit.command(RevObjectParse.class).setRefSpec("HEAD:way").call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);
        // check that the tree with the mapping exist and is not empty
        tree = geogit.command(RevObjectParse.class).setRefSpec("HEAD:onewaystreets")
                .call(RevTree.class);
        assertTrue(tree.isPresent());
        assertTrue(tree.get().size() > 0);

        // check that the mapping was correctly performed
        Optional<Node> feature = geogit.getRepository().getWorkingTree()
                .findUnstaged("onewaystreets/31045880");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
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

        // Define a mapping
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("oneway", Lists.newArrayList("yes"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.LINESTRING));
        fields.put("lit", new AttributeDefinition("lit", FieldType.STRING));
        Map<String, List<String>> filterExclude = Maps.newHashMap();
        MappingRule mappingRule = new MappingRule("onewaystreets", mappings, filterExclude, fields,
                null);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);

        // import with mapping and check import went ok and canonical folders were not created
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).setMapping(mapping)
                .setNoRaw(true).call();
        long unstaged = geogit.getRepository().getWorkingTree().countUnstaged("node").getCount();
        assertEquals(0, unstaged);
        unstaged = geogit.getRepository().getWorkingTree().countUnstaged("way").getCount();
        assertEquals(0, unstaged);
        unstaged = geogit.getRepository().getWorkingTree().countUnstaged("onewaystreets")
                .getCount();
        assertEquals(2, unstaged);
        Optional<Node> feature = geogit.getRepository().getWorkingTree()
                .findUnstaged("onewaystreets/31045880");
        assertTrue(feature.isPresent());

        // check that the mapping was correctly performed
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        String wkt = "LINESTRING (7.1923367 50.7395887, 7.1923127 50.7396946, 7.1923444 50.7397419, 7.1924199 50.7397781)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals("31045880", values.get(0).get().toString());
        assertEquals("yes", values.get(1).get());

        // check it has not created mapping log files
        File osmMapFolder = geogit.command(ResolveOSMMappingLogFolder.class).call();
        file = new File(osmMapFolder, "onewaystreets");
        assertFalse(file.exists());
        file = new File(osmMapFolder, geogit.getRepository().getWorkingTree().getTree().getId()
                .toString());
        assertFalse(file.exists());
    }

}
