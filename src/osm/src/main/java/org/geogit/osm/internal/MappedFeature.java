package org.geogit.osm.internal;

import org.opengis.feature.Feature;

public class MappedFeature {

    private Feature feature;

    private String path;

    public MappedFeature(String path, Feature feature) {
        this.path = path;
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }

    public String getPath() {
        return path;
    }

}