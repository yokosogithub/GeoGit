/* Copyright (c) 2011-2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.CreateTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.storage.BlobPrinter;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectInserter;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.RefDatabase;

import com.google.common.base.Optional;
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
    private ConfigDatabase configDatabase;

    @Inject
    private RefDatabase refDatabase;

    @Inject
    private ObjectDatabase objectDatabase;

    public Repository() {
    }

    public void create() {
        refDatabase.create();
        objectDatabase.open();
        index.getDatabase().open();
    }

    public ConfigDatabase getConfigDatabase() {
        return configDatabase;
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

    /**
     * Test if a blob exists in the object database
     * 
     * @param id the ID of the blob in the object database
     * @return true if the blob exists with the parameter ID, false otherwise
     */
    public boolean blobExists(final ObjectId id) {
        return getObjectDatabase().exists(id);
    }

    /**
     */
    public Optional<Ref> getRef(final String revStr) {
        Optional<Ref> ref = command(RefParse.class).setName(revStr).call();
        return ref;
    }

    /**
     */
    public Optional<Ref> getHead() {
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
     * @return an {@link ObjectInserter} to insert objects into the object database
     */
    public ObjectInserter newObjectInserter() {
        return getObjectDatabase().newObjectInserter();
    }

    public RevFeature getFeature(final RevFeatureType featureType, final String featureId,
            final ObjectId contentId) {
        ObjectReader<RevFeature> reader = newFeatureReader(featureType, featureId);

        RevFeature revFeature = getObjectDatabase().get(contentId, reader);

        return revFeature;
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

    public ObjectReader<RevFeature> newFeatureReader(RevFeatureType featureType, String featureId) {
        ObjectReader<RevFeature> reader = serialFactory.createFeatureReader(featureType, featureId);
        return reader;
    }

    public ObjectReader<RevFeature> newFeatureReader(final RevFeatureType featureType,
            final String featureId, final Map<String, Serializable> hints) {
        return serialFactory.createFeatureReader(featureType, featureId, hints);
    }

    public ObjectWriter<RevFeature> newFeatureWriter(RevFeature feature) {
        return serialFactory.createFeatureWriter(feature);
    }

    public ObjectWriter<RevFeatureType> newFeatureTypeWriter(RevFeatureType type) {
        return serialFactory.createFeatureTypeWriter(type);
    }

    public ObjectReader<RevFeatureType> newFeatureTypeReader() {
        return serialFactory.createFeatureTypeReader();
    }

    public ObjectSerialisingFactory getSerializationFactory() {
        return serialFactory;
    }

    /**
     * @return
     */
    public RevTree getOrCreateHeadTree() {
        ObjectId headTreeId = command(ResolveTreeish.class).setTreeish(Ref.HEAD).call();
        if (headTreeId.isNull()) {
            return command(CreateTree.class).call();
        }
        return getTree(headTreeId);
    }

    /**
     * @param treeId
     * @return
     */
    public RevTree getTree(ObjectId treeId) {
        return (RevTree) command(RevObjectParse.class).setObjectId(treeId).call();
    }

    public Optional<NodeRef> getRootTreeChild(String path) {
        return command(FindTreeChild.class).setChildPath(path).call();
    }

    public Optional<NodeRef> getTreeChild(RevTree tree, String childPath) {
        return command(FindTreeChild.class).setParent(tree).setChildPath(childPath).call();
    }

}
