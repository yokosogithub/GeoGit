/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.map.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.geogit.osm.base.OSMUtils;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

/**
 * A function that transforms a feature representign an OSM entity into a feature with a given
 * feature type, according to a set of mapping rules.
 * 
 */
public class Mapping {

    @Expose
    private List<MappingRule> rules;

    public Mapping(List<MappingRule> rules) {
        this.rules = rules;
    }

    /**
     * Transforms the passed feature according to the mapping rules of this mapping. If several
     * rules can be applied, only the first one found is used.
     * 
     * If no rule can be applied, Optional.absent is returned
     * 
     * @param feature the feature to transform
     * @return
     */
    public Optional<MappedFeature> map(Feature feature) {
        String tagsString = (String) ((SimpleFeature) feature).getAttribute("tags");
        Collection<Tag> tags = OSMUtils.buildTagsCollectionFromString(tagsString);
        for (MappingRule rule : rules) {
            Optional<Feature> newFeature = rule.apply(feature, tags);
            if (newFeature.isPresent()) {
                return Optional.of(new MappedFeature(rule.getName(), newFeature.get()));
            }
        }
        return Optional.absent();
    }

    /**
     * Returns true if this any of the rules in this mapping can be used to convert the passed
     * feature
     * 
     * @param feature
     * @return
     */
    public boolean canBeApplied(Feature feature) {
        String tagsString = (String) ((SimpleFeature) feature).getAttribute("tags");
        Collection<Tag> tags = OSMUtils.buildTagsCollectionFromString(tagsString);
        if (tags.isEmpty()) {
            return false;
        }
        for (MappingRule rule : rules) {
            if (rule.canBeApplied(feature, tags)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads a mapping from a file that contains a JSON representation of it
     * 
     * @param filepath the path of the mapping file
     * @return
     */
    public static Mapping fromFile(String filepath) {
        File mappingFile = new File(filepath);

        Preconditions.checkArgument(mappingFile.exists(),
                "The specified mapping file does not exist");

        String mappingJson;
        try {
            mappingJson = readFile(mappingFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading mapping file:" + e.getMessage(), e);
        }

        GsonBuilder gsonBuilder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
        Gson gson = gsonBuilder.create();
        Mapping mapping;
        try {
            mapping = gson.fromJson(mappingJson, Mapping.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Error parsing mapping definition: "
                    + e.getMessage(), e);
        }

        return mapping;

    }

    private static String readFile(File file) throws IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public List<MappingRule> getRules() {
        return rules;
    }

    /**
     * Returns true if any of the rules in this mapping generates lines or polygons, so it needs
     * ways as inputs
     * 
     * @return
     */
    public boolean canUseWays() {
        for (MappingRule rule : rules) {
            if (rule.canUseWays()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any of the rules in this mapping generates points, so it nodes ways as inputs
     * 
     * @return
     */
    public boolean canUseNodes() {
        for (MappingRule rule : rules) {
            if (rule.canUseNodes()) {
                return true;
            }
        }
        return false;
    }

}
