package org.geogit.api.plumbing.diff;

import org.geogit.api.RevFeatureType;
import org.opengis.feature.Feature;

/**
 * A class to store the information about a feature included in a patch, to be added or removed
 * 
 */
public class PatchFeature {

    private Feature feature;

    private RevFeatureType featureType;

    private String path;

    public PatchFeature(Feature feature, RevFeatureType featureType, String path) {
        this.path = path;
        this.feature = feature;
        this.featureType = featureType;
    }

    /**
     * The feature
     * 
     * @return
     */
    public Feature getFeature() {
        return feature;
    }

    /**
     * The feature type of the feature
     * 
     * @return
     */
    public RevFeatureType getFeatureType() {
        return featureType;
    }

    /**
     * The path to where the feature is to be added
     * 
     * @return
     */
    public String getPath() {
        return path;
    }

}