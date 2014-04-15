/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.diff;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geogit.api.FeatureBuilder;
import org.geogit.api.FeatureInfo;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.text.TextSerializationFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Serializes the differences between two versions of the repository, in plain text format
 * 
 * This is a very basic first version used to test patches created using a plain text format similar
 * to the format output by the diff command. This should be extended to support other formats.
 * 
 */
public class PatchSerializer {

    private static TextSerializationFactory factory = new TextSerializationFactory();

    /**
     * Creates a patch object to apply on a GeoGit working tree
     * 
     * @param reader the read from where to read the patch description
     * @return a Patch
     */
    public static Patch read(BufferedReader reader) {
        Preconditions.checkNotNull(reader);

        Patch patch = new Patch();
        List<String> subset = Lists.newArrayList();
        Map<String, RevFeatureType> featureTypes = Maps.newHashMap();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() && !subset.isEmpty()) {
                    addElement(subset, patch, featureTypes);
                    subset.clear();
                } else if (!line.isEmpty()) {
                    subset.add(line);
                }
            }
            if (!subset.isEmpty()) {
                addElement(subset, patch, featureTypes);
            }
            Set<Entry<String, RevFeatureType>> entries = featureTypes.entrySet();
            for (Iterator<Entry<String, RevFeatureType>> iterator = entries.iterator(); iterator
                    .hasNext();) {
                Entry<String, RevFeatureType> entry = iterator.next();
                patch.addFeatureType(entry.getValue());
            }
            return patch;
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read patch: " + e.getMessage());
        }
    }

    private static void addElement(List<String> lines, Patch patch,
            Map<String, RevFeatureType> featureTypes) {
        String[] headerTokens = lines.get(0).split("\t");
        if (headerTokens.length == 4 || headerTokens.length == 3) {// feature or feature type
                                                                   // modified // modification
            if (lines.size() == 1) { // feature type
                FeatureTypeDiff diff = new FeatureTypeDiff(headerTokens[0],
                        ObjectId.valueOf(headerTokens[1]), ObjectId.valueOf(headerTokens[2]));
                patch.addAlteredTree(diff);
            } else {// feature
                String element = Joiner.on("\n").join(lines.subList(1, lines.size()));
                ByteArrayInputStream stream;
                stream = new ByteArrayInputStream(element.getBytes(Charsets.UTF_8));
                String operation = headerTokens[0].trim();
                if (operation.equals("M")) {
                    String fullPath = headerTokens[1].trim();
                    String oldMetadataId = headerTokens[2].trim();
                    String newMetadataId = headerTokens[3].trim();
                    RevFeatureType newRevFeatureType = featureTypes.get(newMetadataId);
                    RevFeatureType oldRevFeatureType = featureTypes.get(oldMetadataId);

                    Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
                    for (int i = 1; i < lines.size(); i++) {
                        addDifference(lines.get(i), map, oldRevFeatureType, newRevFeatureType);
                    }
                    FeatureDiff featureDiff = new FeatureDiff(fullPath, map, oldRevFeatureType,
                            newRevFeatureType);
                    patch.addModifiedFeature(featureDiff);
                } else if (operation.equals("A") || operation.equals("R")) {
                    String fullPath = headerTokens[1].trim();
                    String featureTypeId = headerTokens[2].trim();
                    RevFeatureType revFeatureType;
                    revFeatureType = featureTypes.get(featureTypeId);
                    FeatureBuilder featureBuilder = new FeatureBuilder(revFeatureType);
                    ObjectReader<RevFeature> reader = factory.createFeatureReader();
                    RevFeature revFeature = reader.read(null, stream);
                    Feature feature = featureBuilder.build(NodeRef.nodeFromPath(fullPath),
                            revFeature);
                    if (operation.equals("R")) {
                        patch.addRemovedFeature(fullPath, feature, revFeatureType);
                    } else {
                        patch.addAddedFeature(fullPath, feature, revFeatureType);
                    }
                } else {
                    throw new IllegalArgumentException("Wrong patch content: " + lines.get(0));
                }
            }

        } else if (headerTokens.length == 1) {// feature type definition
            String element = Joiner.on("\n").join(lines);
            ByteArrayInputStream stream = new ByteArrayInputStream(element.getBytes(Charsets.UTF_8));
            String[] tokens = lines.get(1).split("\t");
            ObjectReader<RevFeatureType> reader = factory.createFeatureTypeReader();
            RevFeatureType featureType = reader.read(null, stream);
            featureTypes.put(featureType.getId().toString(), featureType);
        } else {
            throw new IllegalArgumentException("Wrong patch content: " + lines.get(0));
        }

    }

    private static void addDifference(String s, Map<PropertyDescriptor, AttributeDiff> map,
            RevFeatureType oldRevFeatureType, RevFeatureType newRevFeatureType) {
        String[] tokens = s.split("\t");
        PropertyDescriptor descriptor = oldRevFeatureType.type().getDescriptor(tokens[0]);
        if (descriptor == null) {
            descriptor = newRevFeatureType.type().getDescriptor(tokens[0]);
        }
        AttributeDiff ad = AttributeDiffFactory.attributeDiffFromText(descriptor.getType()
                .getBinding(), s.substring(s.indexOf("\t") + 1));
        map.put(descriptor, ad);
    }

    public static void write(Writer w, Patch patch) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<RevFeatureType> featureTypes = patch.getFeatureTypes();
        for (RevFeatureType featureType : featureTypes) {
            ObjectWriter<RevObject> writer = factory.createObjectWriter(TYPE.FEATURETYPE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            writer.write(featureType, output);
            sb.append(output.toString());
            sb.append('\n');
        }

        TextSerializationFactory factory = new TextSerializationFactory();
        for (FeatureInfo feature : patch.getAddedFeatures()) {
            String path = feature.getPath();
            sb.append("A\t" + path + "\t" + feature.getFeatureType().getId() + "\n");
            ObjectWriter<RevObject> writer = factory.createObjectWriter(TYPE.FEATURE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            RevFeature revFeature = new RevFeatureBuilder().build(feature.getFeature());
            try {
                writer.write(revFeature, output);
            } catch (IOException e) {
            }
            sb.append(output.toString());
            sb.append('\n');
        }
        for (FeatureInfo feature : patch.getRemovedFeatures()) {
            String path = feature.getPath();
            sb.append("R\t" + path + "\t" + feature.getFeatureType().getId() + "\n");
            ObjectWriter<RevObject> writer = factory.createObjectWriter(TYPE.FEATURE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            RevFeature revFeature = new RevFeatureBuilder().build(feature.getFeature());
            try {
                writer.write(revFeature, output);
            } catch (IOException e) {
            }
            sb.append(output.toString());
            sb.append('\n');
        }
        for (FeatureDiff diff : patch.getModifiedFeatures()) {
            sb.append("M\t" + diff.getPath() + "\t" + diff.getOldFeatureType().getId().toString()
                    + "\t" + diff.getNewFeatureType().getId().toString() + "\n");
            sb.append(diff.asText() + "\n");
        }
        for (FeatureTypeDiff diff : patch.getAlteredTrees()) {
            sb.append(diff.toString() + "\n");
        }

        w.write(sb.toString());
        w.flush();
    }

}
