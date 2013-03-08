/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.ObjectWriter;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * The HessianFactory is used to create instances of the various writers and readers used to work
 * with the serialized forms of various repository elements in the hessian format.
 * 
 */
public class HessianFactory implements ObjectSerializingFactory {

    /** generic revobject reader */
    private static final HessianRevObjectReader OBJECT_READER = new HessianRevObjectReader();

    private static final HessianSimpleFeatureTypeReader SIMPLE_FEATURE_TYPE_READER = new HessianSimpleFeatureTypeReader();

    private static final HessianSimpleFeatureTypeWriter SIMPLE_FEATURE_TYPE_WRITER = new HessianSimpleFeatureTypeWriter();

    private static final HessianFeatureReader FEATURE_READER = new HessianFeatureReader(null);

    private static final HessianFeatureWriter FEATURE_WRITER = new HessianFeatureWriter();

    private static final HessianCommitReader COMMIT_READER = new HessianCommitReader();

    private static final HessianCommitWriter COMMIT_WRITER = new HessianCommitWriter();

    private static final HessianRevTreeReader TREE_READER = new HessianRevTreeReader();

    private static final HessianRevTreeWriter TREE_WRITER = new HessianRevTreeWriter();

    private static final HessianRevTagReader TAG_READER = new HessianRevTagReader();

    private static final ObjectWriter<RevTag> TAG_WRITER = new ObjectWriter<RevTag>() {

        @Override
        public void write(RevTag object, OutputStream out) throws IOException {
            throw new UnsupportedOperationException();
        }
    };

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
     * Creates an instance of a Feature reader that can parse features.
     * 
     * @return feature reader
     */
    @Override
    public ObjectReader<RevFeature> createFeatureReader() {
        ObjectReader<RevFeature> reader = FEATURE_READER;
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

    @Override
    public ObjectReader<RevTree> createRevTreeReader() {
        return TREE_READER;
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

    private static final ImmutableList<ObjectWriter<? extends RevObject>> WRITERS;
    static {
        TreeMap<Integer, ObjectWriter<? extends RevObject>> writers = Maps.newTreeMap();
        for (RevObject.TYPE type : RevObject.TYPE.values()) {
            int ordinal = type.ordinal();
            ObjectWriter<? extends RevObject> writer;
            switch (type) {
            case COMMIT:
                writer = COMMIT_WRITER;
                break;
            case FEATURE:
                writer = FEATURE_WRITER;
                break;
            case FEATURETYPE:
                writer = SIMPLE_FEATURE_TYPE_WRITER;
                break;
            case TREE:
                writer = TREE_WRITER;
                break;
            case TAG:
                writer = TAG_WRITER;
                break;
            default:
                throw new UnsupportedOperationException("No writer defined for " + type);
            }

            writers.put(Integer.valueOf(ordinal), writer);
        }
        WRITERS = ImmutableList.copyOf(writers.values());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObjectWriter<RevObject> createObjectWriter(TYPE type) {
        return (ObjectWriter<RevObject>) WRITERS.get(type.ordinal());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ObjectReader<T> createObjectReader(TYPE type) {
        switch (type) {
        case COMMIT:
            return (ObjectReader<T>) COMMIT_READER;
        case FEATURE:
            return (ObjectReader<T>) FEATURE_READER;
        case FEATURETYPE:
            return (ObjectReader<T>) SIMPLE_FEATURE_TYPE_READER;
        case TAG:
            throw new UnsupportedOperationException();
        case TREE:
            return (ObjectReader<T>) TREE_READER;
        default:
            throw new IllegalArgumentException("Unkown type: " + type);
        }
    }

    @Override
    public ObjectReader<RevObject> createObjectReader() {
        return OBJECT_READER;
    }

    private static final class HessianRevObjectReader extends HessianRevReader<RevObject> implements
            ObjectReader<RevObject> {

        @Override
        protected RevObject read(ObjectId id, Hessian2Input hin, RevObject.TYPE type)
                throws IOException {
            switch (type) {
            case COMMIT:
                return COMMIT_READER.read(id, hin, type);
            case FEATURE:
                return FEATURE_READER.read(id, hin, type);
            case TREE:
                return TREE_READER.read(id, hin, type);
            case FEATURETYPE:
                return SIMPLE_FEATURE_TYPE_READER.read(id, hin, type);
            default:
                throw new IllegalArgumentException("Unknown object type " + type);
            }
        }
    }
}
