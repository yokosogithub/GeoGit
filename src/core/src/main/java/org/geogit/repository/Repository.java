/* Copyright (c) 2011-2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevObject;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.storage.BlobPrinter;
import org.geogit.storage.BlobReader;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RefDatabase;
import org.geotools.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * A repository is a collection of commits, each of which is an archive of what the project's
 * working tree looked like at a past date, whether on your machine or someone else's.
 * <p>
 * It also defines HEAD (see below), which identifies the branch or commit the current working tree
 * stemmed from. Lastly, it contains a set of branches and tags, to identify certain commits by
 * name.
 * </p>
 * 
 * @author Gabriel Roldan
 * @see WorkingTree
 */
public class Repository {

    @Inject
    private StagingArea index;

    @Inject
    private WorkingTree workingTree;

    @Inject
    private ObjectSerialisingFactory serialFactory;

    @Inject
    private Injector injector;

    @Inject
    private RefDatabase refDatabase;

    @Inject
    private ObjectDatabase objectDatabase;

    public Repository() {
    }

    public void create() {
        refDatabase.create();
        objectDatabase.create();
        index.getDatabase().create();
    }

    public RefDatabase getRefDatabase() {
        return refDatabase;
    }

    public ObjectDatabase getObjectDatabase() {
        return objectDatabase;
    }

    public StagingArea getIndex() {
        return index;
    }

    public void close() {
        refDatabase.close();
        objectDatabase.close();
        index.getDatabase().close();
    }

    /**
     * @param commandClass
     */
    public <T extends AbstractGeoGitOp<?>> T command(Class<T> commandClass) {
        return injector.getInstance(commandClass);
    }

    public WorkingTree getWorkingTree() {
        return workingTree;
    }

    public InputStream getRawObject(final ObjectId oid) throws IOException {
        return getObjectDatabase().getRaw(oid);
    }

    @SuppressWarnings("unchecked")
    public <T extends RevObject> T resolve(final String revstr, Class<T> type) {
        Ref ref = getRef(revstr);
        if (ref != null) {
            RevObject parsed = parse(ref);
            if (!type.isAssignableFrom(parsed.getClass())) {
                return (T) parsed;
            }
        }

        // not a ref name, may be a partial object id?
        List<ObjectId> lookUp = getObjectDatabase().lookUp(revstr);
        if (!lookUp.isEmpty()) {
            for (ObjectId oid : lookUp) {
                try {
                    if (RevCommit.class.equals(type) || RevTag.class.equals(type)) {
                        return (T) getCommit(oid);
                    } else if (RevTree.class.equals(type)) {
                        return (T) getTree(oid);
                    } else if (RevFeature.class.equals(type)) {
                        return (T) getBlob(oid);
                    }
                } catch (Exception wrongType) {
                    // ignore
                }
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * @param revstr an object reference expression
     * @return the object the resolved reference points to.
     * @throws NoSuchElementException if {@code revstr} dosn't resolve to any object
     * @throws IllegalArgumentException if {@code revstr} resolves to more than one object.
     * @deprecated use revpa
     */
    // public RevObject resolve(final String revstr) {
    // Ref ref = getRef(revstr);
    // if (ref != null) {
    // return parse(ref);
    // }
    //
    // // not a ref name, may be a partial object id?
    // List<ObjectId> lookUp = getObjectDatabase().lookUp(revstr);
    // if (lookUp.size() == 1) {
    // final ObjectId objectId = lookUp.get(0);
    // try {
    // return getCommit(objectId);
    // } catch (Exception e) {
    // try {
    // return getTree(objectId);
    // } catch (Exception e2) {
    // return getBlob(objectId);
    // }
    // }
    // }
    // if (lookUp.size() > 1) {
    // throw new IllegalArgumentException("revstr '" + revstr
    // + "' resolves to more than one object: " + lookUp);
    // }
    // throw new NoSuchElementException();
    // }

    public RevFeature getBlob(ObjectId objectId) {
        return getObjectDatabase().get(objectId, new BlobReader());
    }

    /**
     * Test if a blob exists in the object database
     * 
     * @param id the ID of the blob in the object database
     * @return true if the blob exists with the parameter ID, false otherwise
     */
    public boolean blobExists(final ObjectId id) {
        try {
            getBlob(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private RevObject parse(final Ref ref) {
        final ObjectDatabase objectDatabase = getObjectDatabase();
        switch (ref.getType()) {
        case BLOB:
            return getBlob(ref.getObjectId());
        case COMMIT:
        case TAG:
            return getCommit(ref.getObjectId());
        case TREE:
            return getTree(ref.getObjectId());
        default:
            throw new IllegalArgumentException("Unknown ref type: " + ref);
        }
    }

    /**
     */
    public Ref getRef(final String revStr) {
        Ref ref = command(RefParse.class).setName(revStr).call();
        return ref;
    }

    /**
     */
    public Ref getHead() {
        return getRef(Ref.HEAD);
    }

    public boolean commitExists(final ObjectId id) {
        try {
            getObjectDatabase().get(id, newCommitReader());
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    public RevCommit getCommit(final ObjectId commitId) {
        RevCommit commit = getObjectDatabase().get(commitId, newCommitReader());

        return commit;
    }

    public RevTree getTree(final ObjectId treeId) {
        if (treeId.isNull()) {
            return newTree();
        }
        RevTree tree;
        ObjectDatabase odb = getObjectDatabase();
        tree = odb.get(treeId, newRevTreeReader(odb));

        return tree;
    }

    /**
     * Test if a tree exists in the object database
     * 
     * @param id the ID of the tree in the object database
     * @return true if the tree exists with the parameter ID, false otherwise
     */
    public boolean treeExists(final ObjectId id) {
        try {
            getObjectDatabase().get(id, newRevTreeReader(getObjectDatabase()));
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    public ObjectId getRootTreeId() {
        // find the root tree
        ObjectId commitId = command(RevParse.class).setRefSpec(Ref.HEAD).call();
        if (commitId.isNull()) {
            return commitId;
        }
        RevCommit commit = (RevCommit) command(RevObjectParse.class)
                .setRefSpec(commitId.toString()).call();
        ObjectId treeId = commit.getTreeId();
        return treeId;
    }

    /**
     * @return the root tree for the current HEAD
     */
    public RevTree getHeadTree() {

        RevTree root;

        ObjectId rootTreeId = getRootTreeId();
        if (rootTreeId.isNull()) {
            return newTree();
        }
        root = getObjectDatabase().get(rootTreeId, newRevTreeReader(getObjectDatabase()));

        return root;
    }

    /**
     * @return an {@link ObjectInserter} to insert objects into the object database
     */
    public ObjectInserter newObjectInserter() {
        return getObjectDatabase().newObjectInserter();
    }

    public Feature getFeature(final FeatureType featureType, final String featureId,
            final ObjectId contentId) {
        ObjectReader<Feature> reader = newFeatureReader(featureType, featureId);

        Feature feature = getObjectDatabase().get(contentId, reader);

        return feature;
    }

    /**
     * Creates and return a new, empty tree, that stores to this repository's {@link ObjectDatabase}
     */
    public RevTree newTree() {
        return getObjectDatabase().newTree();
    }

    public NodeRef getRootTreeChild(List<String> path) {
        RevTree root = getHeadTree();
        return getObjectDatabase().getTreeChild(root, path);
    }

    public NodeRef getRootTreeChild(String... path) {
        RevTree root = getHeadTree();
        return getObjectDatabase().getTreeChild(root, path);
    }

    public ObjectWriter<RevCommit> newCommitWriter(RevCommit commit) {
        return serialFactory.createCommitWriter(commit);
    }

    public BlobPrinter newBlobPrinter() {
        return serialFactory.createBlobPrinter();
    }

    public ObjectReader<RevTree> newRevTreeReader(ObjectDatabase objectDatabase) {
        return serialFactory.createRevTreeReader(objectDatabase);
    }

    public ObjectReader<RevTree> newRevTreeReader(ObjectDatabase odb, int depth) {
        return serialFactory.createRevTreeReader(odb, depth);
    }

    public ObjectWriter<RevTree> newRevTreeWriter(RevTree tree) {
        return serialFactory.createRevTreeWriter(tree);
    }

    public ObjectReader<RevCommit> newCommitReader() {
        return serialFactory.createCommitReader();
    }

    public ObjectReader<Feature> newFeatureReader(FeatureType featureType, String featureId) {
        return serialFactory.createFeatureReader(featureType, featureId);
    }

    public ObjectReader<Feature> newFeatureReader(final FeatureType featureType,
            final String featureId, final Hints hints) {
        return serialFactory.createFeatureReader(featureType, featureId, hints);
    }

    public ObjectWriter<Feature> newFeatureWriter(Feature feature) {
        return serialFactory.createFeatureWriter(feature);
    }

    public ObjectWriter<SimpleFeatureType> newSimpleFeatureTypeWriter(SimpleFeatureType type) {
        return serialFactory.createSimpleFeatureTypeWriter(type);
    }

    public ObjectReader<SimpleFeatureType> newSimpleFeatureTypeReader() {
        return serialFactory.createSimpleFeatureTypeReader();
    }

    public ObjectSerialisingFactory getSerializationFactory() {
        return serialFactory;
    }

}
