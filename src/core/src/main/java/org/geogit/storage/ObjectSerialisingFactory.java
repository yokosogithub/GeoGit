/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.Serializable;
import java.util.Map;

import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;

/**
 * The ObjectSerialisingFactory is used to create instances of the various writers and readers used
 * to work with the serialized forms of various repository elements.
 * 
 * @author mleslie
 * 
 */
public interface ObjectSerialisingFactory {

    /**
     * Creates an instance of a commit writer to serialise the provided RevCommit
     * 
     * @param commit RevCommit to be written
     * @return commit writer
     */
    public ObjectWriter<RevCommit> createCommitWriter(final RevCommit commit);

    /**
     * Creates an instance of a commit reader.
     * 
     * @return commit reader
     */
    public ObjectReader<RevCommit> createCommitReader();

    /**
     * Creates an instance of a RevTree writer to serialise the provided RevTree
     * 
     * @param tree RevTree to be written
     * @return revtree writer
     */
    public ObjectWriter<RevTree> createRevTreeWriter(RevTree tree);

    /**
     * Creates an instance of a RevTree reader.
     * 
     * @param objectDb the ObjectDatabase the RevTree is stored in
     * @return revtree reader
     */
    public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb);

    /**
     * Creates an instance of a RevTree reader that will start reading a the given tree depth.
     * 
     * @param objectDb the ObjectDatabase the RevTree is stored in
     * @param depth depth of the revtree's root
     * @return revtree reader
     */
    public ObjectReader<RevTree> createRevTreeReader(ObjectDatabase objectDb, int depth);

    /**
     * Creates an instance of a Feature writer to serialize the provided feature.
     * 
     * @param feature Feature to be written
     * @return feature writer
     */
    public ObjectWriter<RevFeature> createFeatureWriter(final RevFeature feature);

    /**
     * Creates an instance of a Feature reader that can parse features.
     * 
     * @return feature reader
     */
    public ObjectReader<RevFeature> createFeatureReader();

    /**
     * Creates an instance of a Feature reader that can parse features.
     * 
     * @param hints feature creation hints
     * @return feature reader
     */
    public ObjectReader<RevFeature> createFeatureReader(final Map<String, Serializable> hints);

    /**
     * Creates a BlobPrinter that can parse serialised elements into a human-readable(ish)
     * representation, typically xml.
     * 
     * @return instance of a BlobPrinter for the current serialisation
     */
    public BlobPrinter createBlobPrinter();

    /**
     * Creates an instance of a feature type writer to serialize the provided feature type.
     * 
     * @param type the feature type to write
     * @return feature type writer
     */
    public ObjectWriter<RevFeatureType> createFeatureTypeWriter(RevFeatureType type);

    /**
     * Creates an instance of a feature type reader that can parse feature types.
     * 
     * @return feature type reader
     */
    public ObjectReader<RevFeatureType> createFeatureTypeReader();

    /**
     * Creates an instance of an object type reader that can determine the type of objects.
     * 
     * @return object type reader
     */
    public ObjectReader<RevObject.TYPE> createObjectTypeReader();
}
