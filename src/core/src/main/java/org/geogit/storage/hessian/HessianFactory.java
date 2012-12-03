/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.Serializable;
import java.util.Map;

import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * The HessianFactory is used to create instances of the various writers and readers used to work
 * with the serialized forms of various repository elements in the hessian format.
 * 
 */
public class HessianFactory implements ObjectSerialisingFactory {

    /** HESSIAN_OBJECT_TYPE_READER */
    private static final HessianObjectTypeReader OBJECT_TYPE_READER = new HessianObjectTypeReader();

    /** HESSIAN_SIMPLE_FEATURE_TYPE_READER */
    private static final HessianSimpleFeatureTypeReader SIMPLE_FEATURE_TYPE_READER = new HessianSimpleFeatureTypeReader();

    /** HESSIAN_COMMIT_READER */
    private static final HessianCommitReader COMMIT_READER = new HessianCommitReader();

    /** HESSIAN_TREE_READER */
    private static final HessianRevTreeReader TREE_READER = new HessianRevTreeReader();

    /**
     * Creates an instance of a commit reader.
     * 
     * @return commit reader
     */
    @Override
    public ObjectReader<RevCommit> createCommitReader() {
        return COMMIT_READER;
    }

    /**
     * Creates an instance of a commit writer to serialise the provided RevCommit
     * 
     * @param commit RevCommit to be written
     * @return commit writer
     */
    @Override
    public ObjectWriter<RevCommit> createCommitWriter(RevCommit commit) {
        return new HessianCommitWriter(commit);
    }

    /**
     * Creates an instance of a Feature reader that can parse features.
     * 
     * @return feature reader
     */
    @Override
    public ObjectReader<RevFeature> createFeatureReader() {
        ObjectReader<RevFeature> reader = new HessianFeatureReader(null);
        return reader;
    }

    /**
     * Creates an instance of a Feature reader that can parse features.
     * 
     * @param hints feature creation hints
     * @return feature reader
     */
    @Override
    public ObjectReader<RevFeature> createFeatureReader(final Map<String, Serializable> hints) {
        return new HessianFeatureReader(hints);
    }

    /**
     * Creates an instance of a Feature writer to serialize the provided feature.
     * 
     * @param feature Feature to be written
     * @return feature writer
     */
    @Override
    public ObjectWriter<RevFeature> createFeatureWriter(RevFeature feature) {
        return new HessianFeatureWriter(feature);
    }

    @Override
    public ObjectReader<RevTree> createRevTreeReader() {
        return TREE_READER;
    }

    /**
     * Creates an instance of a RevTree writer to serialise the provided RevTree
     * 
     * @param tree RevTree to be written
     * @return revtree writer
     */
    @Override
    public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree) {
        return new HessianRevTreeWriter(tree);
    }

    /**
     * Creates an instance of a feature type writer to serialize the provided feature type.
     * 
     * @param type the feature type to write
     * @return feature type writer
     */
    @Override
    public ObjectWriter<RevFeatureType> createFeatureTypeWriter(RevFeatureType type) {
        return new HessianSimpleFeatureTypeWriter((SimpleFeatureType) type.type());
    }

    /**
     * Creates an instance of a feature type reader that can parse feature types.
     * 
     * @return feature type reader
     */
    @Override
    public ObjectReader<RevFeatureType> createFeatureTypeReader() {
        return SIMPLE_FEATURE_TYPE_READER;
    }

    /**
     * Creates an instance of an object type reader that can determine the type of objects.
     * 
     * @return object type reader
     */
    public ObjectReader<RevObject.TYPE> createObjectTypeReader() {
        return OBJECT_TYPE_READER;
    }

}
