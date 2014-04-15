/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.osm.internal.MappingRule.DefaultField;
import org.geogit.osm.internal.log.OSMMappingLogEntry;
import org.geogit.osm.internal.log.ReadOSMMapping;
import org.geogit.osm.internal.log.ReadOSMMappingLogEntry;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Updates the raw OSM data of the repository (stored in the "node" and "way" trees), with the data
 * in a tree that represents a mapped version of that raw data
 */
public class OSMUnmapOp extends AbstractGeoGitOp<RevTree> {

    /**
     * To avoid having to create a feature from the revFeature object when the unmapped feature
     * already exist, in order to access the field by name, we compute field indexes in advance
     * corresponding to the "way" and "node" types
     */

    private static final RevFeatureType nodeType = RevFeatureType.build(OSMUtils.nodeType());

    private static final RevFeatureType wayType = RevFeatureType.build(OSMUtils.wayType());

    private static final int NODE_TAGS_FIELD_INDEX = getPropertyIndex(nodeType, "tags");

    private static final int NODE_USER_FIELD_INDEX = getPropertyIndex(nodeType, "user");

    private static final int NODE_TIMESTAMP_FIELD_INDEX = getPropertyIndex(nodeType, "timestamp");

    private static final int NODE_VERSION_FIELD_INDEX = getPropertyIndex(nodeType, "version");

    private static final int NODE_CHANGESET_FIELD_INDEX = getPropertyIndex(nodeType, "changeset");

    private static final int NODE_LOCATION_FIELD_INDEX = getPropertyIndex(nodeType, "location");

    private static final int WAY_TAGS_FIELD_INDEX = getPropertyIndex(wayType, "tags");

    private static final int WAY_TIMESTAMP_FIELD_INDEX = getPropertyIndex(wayType, "timestamp");

    private static final int WAY_VERSION_FIELD_INDEX = getPropertyIndex(wayType, "version");

    private static final int WAY_CHANGESET_FIELD_INDEX = getPropertyIndex(wayType, "changeset");

    private static final int WAY_USER_FIELD_INDEX = getPropertyIndex(wayType, "user");

    final String UNKNOWN_USER = "Unknown";

    private static int getPropertyIndex(RevFeatureType type, String name) {

        ImmutableList<PropertyDescriptor> descriptors = type.sortedDescriptors();
        for (int i = 0; i < descriptors.size(); i++) {
            if (descriptors.get(i).getName().getLocalPart().equals(name)) {
                return i;
            }
        }
        // shouldn't reach this
        throw new RuntimeException("wrong field name");
    }

    private String path;

    private Mapping mapping;

    private static GeometryFactory gf = new GeometryFactory();

    /**
     * Sets the path to take the mapped data from
     * 
     * @param path
     * @return
     */
    public OSMUnmapOp setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public RevTree call() {

        Optional<OSMMappingLogEntry> entry = command(ReadOSMMappingLogEntry.class).setPath(path)
                .call();
        if (entry.isPresent()) {
            Optional<Mapping> opt = command(ReadOSMMapping.class).setEntry(entry.get()).call();
            if (opt.isPresent()) {
                mapping = opt.get();
            }
        }

        Iterator<NodeRef> iter = command(LsTreeOp.class).setReference(path)
                .setStrategy(Strategy.FEATURES_ONLY).call();

        FeatureMapFlusher flusher = new FeatureMapFlusher(getWorkTree());
        while (iter.hasNext()) {
            NodeRef node = iter.next();
            RevFeature revFeature = command(RevObjectParse.class).setObjectId(node.objectId())
                    .call(RevFeature.class).get();
            RevFeatureType revFeatureType = command(RevObjectParse.class)
                    .setObjectId(node.getMetadataId()).call(RevFeatureType.class).get();
            List<PropertyDescriptor> descriptors = revFeatureType.sortedDescriptors();
            ImmutableList<Optional<Object>> values = revFeature.getValues();
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
                    (SimpleFeatureType) revFeatureType.type());
            String id = null;
            for (int i = 0; i < descriptors.size(); i++) {
                PropertyDescriptor descriptor = descriptors.get(i);
                if (descriptor.getName().getLocalPart().equals("id")) {
                    id = values.get(i).get().toString();
                }
                Optional<Object> value = values.get(i);
                featureBuilder.set(descriptor.getName(), value.orNull());
            }
            Preconditions.checkNotNull(id, "No 'id' attribute found");
            SimpleFeature feature = featureBuilder.buildFeature(id);
            unmapFeature(feature, flusher);

        }

        flusher.flushAll();

        // The above code will unmap all added or modified elements, but not deleted ones.
        // We now process the deletions, by comparing the current state of the mapped tree
        // with its state just after the mapping was created.

        if (entry.isPresent()) {
            Iterator<DiffEntry> diffs = command(DiffTree.class).setFilterPath(path)
                    .setNewTree(getWorkTree().getTree().getId())
                    .setOldTree(entry.get().getPostMappingId()).call();

            while (diffs.hasNext()) {
                DiffEntry diff = diffs.next();
                if (diff.changeType().equals(DiffEntry.ChangeType.REMOVED)) {

                    ObjectId featureId = diff.getOldObject().getNode().getObjectId();
                    RevFeature revFeature = command(RevObjectParse.class).setObjectId(featureId)
                            .call(RevFeature.class).get();
                    RevFeatureType revFeatureType = command(RevObjectParse.class)
                            .setObjectId(diff.getOldObject().getMetadataId())
                            .call(RevFeatureType.class).get();
                    List<PropertyDescriptor> descriptors = revFeatureType.sortedDescriptors();
                    ImmutableList<Optional<Object>> values = revFeature.getValues();
                    SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
                            (SimpleFeatureType) revFeatureType.type());
                    String id = null;
                    for (int i = 0; i < descriptors.size(); i++) {
                        PropertyDescriptor descriptor = descriptors.get(i);
                        if (descriptor.getName().getLocalPart().equals("id")) {
                            id = values.get(i).get().toString();
                        }
                        Optional<Object> value = values.get(i);
                        featureBuilder.set(descriptor.getName(), value.orNull());
                    }
                    Preconditions.checkNotNull(id, "No 'id' attribute found");
                    SimpleFeature feature = featureBuilder.buildFeature(id);
                    Class<?> clazz = feature.getDefaultGeometryProperty().getType().getBinding();
                    String deletePath = clazz.equals(Point.class) ? OSMUtils.NODE_TYPE_NAME
                            : OSMUtils.WAY_TYPE_NAME;
                    getWorkTree().delete(deletePath, id);
                }
            }
        }

        return getWorkTree().getTree();

    }

    private void unmapFeature(SimpleFeature feature, FeatureMapFlusher mapFlusher) {
        Class<?> clazz = feature.getDefaultGeometryProperty().getType().getBinding();
        if (clazz.equals(Point.class)) {
            unmapNode(feature, mapFlusher);
        } else {
            unmapWay(feature, mapFlusher);
        }
    }

    private void unmapNode(SimpleFeature feature, FeatureMapFlusher mapFlusher) {
        boolean modified = false;
        String id = feature.getID();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(OSMUtils.nodeType());
        Optional<RevFeature> rawFeature = command(RevObjectParse.class).setRefSpec(
                "WORK_HEAD:" + OSMUtils.NODE_TYPE_NAME + "/" + id).call(RevFeature.class);
        Map<String, String> tagsMap = Maps.newHashMap();
        long timestamp = System.currentTimeMillis();
        int version = 1;
        long changeset = -1;
        String user = UNKNOWN_USER;
        Collection<Tag> tags = Lists.newArrayList();
        if (rawFeature.isPresent()) {
            ImmutableList<Optional<Object>> values = rawFeature.get().getValues();
            tags = OSMUtils.buildTagsCollectionFromString(values.get(NODE_TAGS_FIELD_INDEX).get()
                    .toString());
            for (Tag tag : tags) {
                tagsMap.put(tag.getKey(), tag.getValue());
            }
            Optional<Object> timestampOpt = values.get(NODE_TIMESTAMP_FIELD_INDEX);
            if (timestampOpt.isPresent()) {
                timestamp = ((Long) timestampOpt.get()).longValue();
            }
            Optional<Object> versionOpt = values.get(NODE_VERSION_FIELD_INDEX);
            if (versionOpt.isPresent()) {
                version = ((Integer) versionOpt.get()).intValue();
            }
            Optional<Object> changesetOpt = values.get(NODE_CHANGESET_FIELD_INDEX);
            if (changesetOpt.isPresent()) {
                changeset = ((Long) changesetOpt.get()).longValue();
            }
            Optional<Object> userOpt = values.get(NODE_USER_FIELD_INDEX);
            if (userOpt.isPresent()) {
                user = (String) userOpt.get();
            }
        }

        Map<String, String> unaliased = Maps.newHashMap();
        Collection<Property> properties = feature.getProperties();
        for (Property property : properties) {
            String name = property.getName().getLocalPart();
            if (name.equals("id")
                    || Geometry.class.isAssignableFrom(property.getDescriptor().getType()
                            .getBinding())) {
                continue;
            }
            Object value = property.getValue();
            if (value != null) {
                String tagName = name;
                if (mapping != null) {
                    if (unaliased.containsKey(name)) {
                        tagName = unaliased.get(name);
                    } else {
                        tagName = mapping.getTagNameFromAlias(path, tagName);
                        unaliased.put(name, tagName);
                    }
                }

                if (!DefaultField.isDefaultField(tagName)) {
                    if (tagsMap.containsKey(tagName) && !modified) {
                        String oldValue = tagsMap.get(tagName);
                        modified = !value.equals(oldValue);
                    }
                    tagsMap.put(tagName, value.toString());
                }
            }
        }

        if (!modified && rawFeature.isPresent()) {
            // no changes after unmapping tags, so there's nothing else to do
            return;
        }

        Collection<Tag> newTags = Lists.newArrayList();
        Set<Entry<String, String>> entries = tagsMap.entrySet();
        for (Entry<String, String> entry : entries) {
            newTags.add(new Tag(entry.getKey(), entry.getValue()));
        }
        featureBuilder.set("tags", OSMUtils.buildTagsString(newTags));
        featureBuilder.set("location", feature.getDefaultGeometry());
        featureBuilder.set("changeset", changeset);
        featureBuilder.set("timestamp", timestamp);
        featureBuilder.set("version", version);
        featureBuilder.set("user", user);
        featureBuilder.set("visible", true);
        if (rawFeature.isPresent()) {
            // the feature has changed, so we cannot reuse some attributes.
            // We reconstruct the feature and insert it
            featureBuilder.set("timestamp", System.currentTimeMillis());
            featureBuilder.set("changeset", null);
            featureBuilder.set("version", null);
            featureBuilder.set("visible", true);
            mapFlusher.put("node", featureBuilder.buildFeature(id));
        } else {
            // The feature didn't exist, so we have to add it
            mapFlusher.put("node", featureBuilder.buildFeature(id));
        }

    }

    /**
     * This method takes a way and generates the corresponding string with ids of nodes than compose
     * that line. If those nodes are declared in the "nodes" attribute of the feature, they will be
     * referenced and no new node will be added. If a coordinate in the linestring doesn't have a
     * corresponding node declared in the "nodes" attribute, a new node at that coordinate will be
     * added to the "node" tree. This way, the returned linestring is guaranteed to refer to nodes
     * that already exist in the repository, and as such can be safely used to add a new way that
     * uses those nodes.
     * 
     * @param line
     * @return
     */
    private String getNodeStringFromWay(SimpleFeature way, FeatureMapFlusher flusher) {

        Map<Coordinate, Long> nodeCoords = Maps.newHashMap();
        Object nodesAttribute = way.getAttribute("nodes");
        if (nodesAttribute != null) {
            String[] nodeIds = nodesAttribute.toString().split(";");
            for (String nodeId : nodeIds) {
                Optional<RevFeature> revFeature = command(RevObjectParse.class).setRefSpec(
                        "WORK_HEAD:" + OSMUtils.NODE_TYPE_NAME + "/" + nodeId).call(
                        RevFeature.class);
                if (revFeature.isPresent()) {
                    Optional<Object> location = revFeature.get().getValues()
                            .get(NODE_LOCATION_FIELD_INDEX);
                    if (location.isPresent()) {
                        Coordinate coord = ((Geometry) location.get()).getCoordinate();
                        nodeCoords.put(coord, Long.parseLong(nodeId));
                    }
                }
            }
        }
        List<Long> nodes = Lists.newArrayList();
        Coordinate[] coords = ((Geometry) way.getDefaultGeometryProperty().getValue())
                .getCoordinates();
        for (Coordinate coord : coords) {
            if (nodeCoords.containsKey(coord)) {
                nodes.add(nodeCoords.get(coord));
            } else {
                nodes.add(createNodeForCoord(coord, flusher));
            }
        }

        return Joiner.on(';').join(nodes);
    }

    /**
     * Creates a new node at a given a given coordinate and inserts it into the working tree
     * 
     * @param coord
     * @return the id of the created node
     */
    private Long createNodeForCoord(Coordinate coord, FeatureMapFlusher flusher) {
        long id = -1 * System.currentTimeMillis(); // TODO: This has to be changed!!!
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(OSMUtils.nodeType());
        featureBuilder.set("tags", null);
        featureBuilder.set("location", gf.createPoint(coord));
        featureBuilder.set("changeset", null);
        featureBuilder.set("timestamp", System.currentTimeMillis());
        featureBuilder.set("version", 1);
        featureBuilder.set("user", null);
        featureBuilder.set("visible", true);
        flusher.put("node", featureBuilder.buildFeature(Long.toString(id)));
        return id;
    }

    private void unmapWay(SimpleFeature feature, FeatureMapFlusher flusher) {
        boolean modified = false;
        String id = feature.getID();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(OSMUtils.wayType());
        Optional<RevFeature> rawFeature = command(RevObjectParse.class).setRefSpec(
                "WORK_HEAD:" + OSMUtils.WAY_TYPE_NAME + "/" + id).call(RevFeature.class);
        Map<String, String> tagsMap = Maps.newHashMap();
        long timestamp = System.currentTimeMillis();
        int version = 1;
        long changeset = -1;
        String user = UNKNOWN_USER;
        Collection<Tag> tags = Lists.newArrayList();
        if (rawFeature.isPresent()) {
            ImmutableList<Optional<Object>> values = rawFeature.get().getValues();
            tags = OSMUtils.buildTagsCollectionFromString(values.get(WAY_TAGS_FIELD_INDEX).get()
                    .toString());
            for (Tag tag : tags) {
                tagsMap.put(tag.getKey(), tag.getValue());
            }
            Optional<Object> timestampOpt = values.get(WAY_TIMESTAMP_FIELD_INDEX);
            if (timestampOpt.isPresent()) {
                timestamp = ((Long) timestampOpt.get()).longValue();
            }
            Optional<Object> versionOpt = values.get(WAY_VERSION_FIELD_INDEX);
            if (versionOpt.isPresent()) {
                version = ((Integer) versionOpt.get()).intValue();
            }
            Optional<Object> changesetOpt = values.get(WAY_CHANGESET_FIELD_INDEX);
            if (changesetOpt.isPresent()) {
                changeset = ((Long) changesetOpt.get()).longValue();
            }
            Optional<Object> userOpt = values.get(WAY_USER_FIELD_INDEX);
            if (userOpt.isPresent()) {
                user = (String) userOpt.get();
            }
        }

        Map<String, String> unaliased = Maps.newHashMap();
        Collection<Property> properties = feature.getProperties();
        for (Property property : properties) {
            String name = property.getName().getLocalPart();
            if (name.equals("id")
                    || name.equals("nodes")
                    || Geometry.class.isAssignableFrom(property.getDescriptor().getType()
                            .getBinding())) {
                continue;
            }
            Object value = property.getValue();
            if (value != null) {
                String tagName = name;
                if (mapping != null) {
                    if (unaliased.containsKey(name)) {
                        tagName = unaliased.get(name);
                    } else {
                        tagName = mapping.getTagNameFromAlias(path, tagName);
                        unaliased.put(name, tagName);
                    }
                }

                if (!DefaultField.isDefaultField(tagName)) {
                    if (tagsMap.containsKey(tagName) && !modified) {
                        String oldValue = tagsMap.get(tagName);
                        modified = !value.equals(oldValue);
                    }
                    tagsMap.put(tagName, value.toString());
                }
            }
        }

        if (!modified && rawFeature.isPresent()) {
            // no changes after unmapping tags, so there's nothing else to do
            return;
        }

        tags.clear();
        Set<Entry<String, String>> entries = tagsMap.entrySet();
        for (Entry<String, String> entry : entries) {
            tags.add(new Tag(entry.getKey(), entry.getValue()));
        }

        Geometry geom = (Geometry) feature.getDefaultGeometry();
        LineString line;
        if (geom instanceof LineString) {
            line = (LineString) geom;
        } else {
            line = gf.createLineString(geom.getCoordinates());
        }
        featureBuilder.set("visible", true);
        featureBuilder.set("tags", OSMUtils.buildTagsString(tags));
        featureBuilder.set("way", line);
        featureBuilder.set("changeset", changeset);
        featureBuilder.set("timestamp", timestamp);
        featureBuilder.set("version", version);
        featureBuilder.set("user", user);
        featureBuilder.set("nodes", getNodeStringFromWay(feature, flusher));
        if (rawFeature.isPresent()) {
            // the feature has changed, so we cannot reuse some attributes
            featureBuilder.set("timestamp", System.currentTimeMillis());
            featureBuilder.set("changeset", -changeset); // temporary negative changeset ID
            // featureBuilder.set("version", version);
            flusher.put("way", featureBuilder.buildFeature(id));
        } else {
            flusher.put("way", featureBuilder.buildFeature(id));
        }

    }

}
