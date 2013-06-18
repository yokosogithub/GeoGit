/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.map.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geogit.api.Node;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.osm.in.internal.OSMImportOp;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.FieldType;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class OSMUnmapOpTest extends RepositoryTestCase {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMappingAndUnmappingOfWays() throws Exception {
        // Import and map
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        Map<String, FieldType> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("highway", Lists.newArrayList("residential"));
        fields.put("geom", FieldType.LINESTRING);
        fields.put("name", FieldType.STRING);
        MappingRule mappingRule = new MappingRule("residential", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        unstaged = workTree.countUnstaged("residential");
        assertEquals(4, unstaged);
        Optional<Node> feature = workTree.findUnstaged("residential/31347480");
        assertTrue(feature.isPresent());
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setObjectId(feature.get().getObjectId()).call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("WORK_HEAD:residential/31347480").call();
        assertTrue(featureType.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(4, values.size());
        // modify a mapped feature
        ArrayList<Coordinate> coords = Lists.newArrayList(((Geometry) values.get(2).get())
                .getCoordinates());
        coords.add(new Coordinate(0, 1));
        assertEquals(31347480l, values.get(0).get());
        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder((SimpleFeatureType) featureType.get()
                .type());
        fb.set("geom", gf.createLineString(coords.toArray(new Coordinate[0])));
        fb.set("name", "newname");
        fb.set("id", 31347480l);
        fb.set("nodes", values.get(3).get());
        SimpleFeature newFeature = fb.buildFeature("31347480");
        geogit.getRepository().getWorkingTree().insert("residential", newFeature);
        Optional<RevFeature> mapped = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:residential/31347480").call(RevFeature.class);
        assertTrue(mapped.isPresent());
        values = mapped.get().getValues();
        assertEquals(
                "LINESTRING (7.1960069 50.7399033, 7.195868 50.7399081, 7.1950788 50.739912, 7.1949262 50.7399053, "
                        + "7.1942463 50.7398686, 7.1935778 50.7398262, 7.1931011 50.7398018, 7.1929987 50.7398009, 7.1925978 50.7397889, "
                        + "7.1924199 50.7397781, 0 1)", values.get(2).get().toString());
        assertEquals(31347480l, ((Long) values.get(0).get()).longValue());
        assertEquals("newname", values.get(1).get().toString());
        // unmap
        geogit.command(OSMUnmapOp.class).setPath("residential").call();
        // check that raw OSM data was updated
        Optional<RevFeature> unmapped = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:way/31347480").call(RevFeature.class);
        assertTrue(unmapped.isPresent());
        values = unmapped.get().getValues();
        assertEquals(
                "LINESTRING (7.1960069 50.7399033, 7.195868 50.7399081, 7.1950788 50.739912, 7.1949262 50.7399053, "
                        + "7.1942463 50.7398686, 7.1935778 50.7398262, 7.1931011 50.7398018, 7.1929987 50.7398009, 7.1925978 50.7397889, "
                        + "7.1924199 50.7397781, 0 1)", values.get(7).get().toString());
        assertEquals("lit:yes|highway:residential|name:newname", values.get(3).get().toString());
        String nodes = values.get(6).get().toString();
        String[] nodeIds = nodes.split(";");
        String newNodeId = nodeIds[nodeIds.length - 1];
        Optional<RevFeature> newNode = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/" + newNodeId).call(RevFeature.class);
        assertTrue(newNode.isPresent());
        values = newNode.get().getValues();
        assertEquals("POINT (0 1)", values.get(6).get().toString());

    }

    @Test
    public void testMappingAndUnmappingOfNodes() throws Exception {
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        WorkingTree workTree = geogit.getRepository().getWorkingTree();
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
        geogit.command(OSMMapOp.class).setMapping(mapping).call();
        unstaged = workTree.countUnstaged("busstops");
        assertEquals(2, unstaged);
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
        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder((SimpleFeatureType) featureType.get()
                .type());
        fb.set("geom", gf.createPoint(new Coordinate(0, 1)));
        fb.set("name", "newname");
        fb.set("id", 507464799l);
        SimpleFeature newFeature = fb.buildFeature("507464799");
        geogit.getRepository().getWorkingTree().insert("busstops", newFeature);
        Optional<RevFeature> mapped = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:busstops/507464799").call(RevFeature.class);
        assertTrue(mapped.isPresent());
        values = mapped.get().getValues();
        assertEquals("POINT (0 1)", values.get(2).get().toString());
        assertEquals(507464799l, ((Long) values.get(0).get()).longValue());
        assertEquals("newname", values.get(1).get().toString());
        geogit.command(OSMUnmapOp.class).setPath("busstops").call();
        Optional<RevFeature> unmapped = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/507464799").call(RevFeature.class);
        assertTrue(unmapped.isPresent());
        values = unmapped.get().getValues();
        assertEquals("POINT (0 1)", values.get(6).get().toString());
        assertEquals(
                "bus:yes|public_transport:platform|highway:bus_stop|VRS:ortsteil:Hoholz|name:newname|VRS:ref:68566|VRS:gemeinde:BONN",
                values.get(3).get().toString());
        // check that unchanged nodes keep their attributes
        Optional<RevFeature> unchanged = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/1633594723").call(RevFeature.class);
        values = unchanged.get().getValues();
        assertEquals("14220478", values.get(4).get().toString());
        assertEquals("1355097351000", values.get(2).get().toString());
        assertEquals("2", values.get(1).get().toString());

    }

    @Test
    public void testUnmappingWithoutIDAttribute() throws Exception {
        insert(points1);
        try {
            geogit.command(OSMUnmapOp.class).setPath("Points").call();
            fail();
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().startsWith("No 'id' attribute found"));
        }

    }

    @Override
    protected void setUpInternal() throws Exception {
        // TODO Auto-generated method stub

    }

}
