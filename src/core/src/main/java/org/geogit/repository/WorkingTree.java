/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.SpatialRef;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.FindOrCreateSubtree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.HashObject;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.WriteBack;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.StagingDatabase;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * A working tree is the collection of Features for a single FeatureType in GeoServer that has a
 * repository associated with it (and hence is subject of synchronization).
 * <p>
 * It represents the set of Features tracked by some kind of geospatial data repository (like the
 * GeoServer Catalog). It is essentially a "tree" with various roots and only one level of nesting,
 * since the FeatureTypes held in this working tree are the equivalents of files in a git working
 * tree.
 * </p>
 * <p>
 * <ul>
 * <li>A WorkingTree represents the current working copy of the versioned feature types
 * <li>A WorkingTree has a Repository
 * <li>A Repository holds commits and branches
 * <li>You perform work on the working tree (insert/delete/update features)
 * <li>Then you commit to the current Repository's branch
 * <li>You can checkout a different branch from the Repository and the working tree will be updated
 * to reflect the state of that branch
 * </ul>
 * 
 * @author Gabriel Roldan
 * @see Repository
 */
public class WorkingTree {

    @Inject
    private StagingDatabase indexDatabase;

    @Inject
    private Repository repository;

    @Inject
    private ObjectSerialisingFactory serialFactory;

    /**
     * Updates the WORK_HEAD ref to the specified tree.
     * 
     * @param newTree the tree to be set as the new WORK_HEAD
     */
    public void updateWorkHead(ObjectId newTree) {
        repository.command(UpdateRef.class).setName(Ref.WORK_HEAD).setNewValue(newTree).call();
    }

    /**
     * @return the tree represented by WORK_HEAD. If there is no tree set at WORK_HEAD, it will
     *         return the HEAD tree (no unstaged changes).
     */
    public RevTree getTree() {
        Optional<ObjectId> workTreeId = repository.command(ResolveTreeish.class)
                .setTreeish(Ref.WORK_HEAD).call();
        final RevTree workTree;
        if (!workTreeId.isPresent() || workTreeId.get().isNull()) {
            // Work tree was not resolved, update it to the head.
            RevTree headTree = repository.getOrCreateHeadTree();
            updateWorkHead(headTree.getId());
            workTree = headTree;

        } else {
            workTree = repository.command(RevObjectParse.class).setObjectId(workTreeId.get())
                    .call(RevTree.class).or(RevTree.EMPTY);
        }
        Preconditions.checkState(workTree != null);
        return workTree;
    }

    /**
     * @return a supplier for the working tree.
     */
    private Supplier<RevTreeBuilder> getTreeSupplier() {
        Supplier<RevTreeBuilder> supplier = new Supplier<RevTreeBuilder>() {
            @Override
            public RevTreeBuilder get() {
                return getTree().builder(indexDatabase);
            }
        };
        return Suppliers.memoize(supplier);
    }

    /**
     * Deletes a single feature from the working tree and updates the WORK_HEAD ref.
     * 
     * @param path the path of the feature
     * @param featureId the id of the feature
     * @return true if the object was found and deleted, false otherwise
     */
    public boolean delete(final String path, final String featureId) {
        RevTreeBuilder parentTree = repository.command(FindOrCreateSubtree.class).setIndex(true)
                .setParent(Suppliers.ofInstance(Optional.of(getTree()))).setChildPath(path).call()
                .builder(indexDatabase);

        String featurePath = NodeRef.appendChild(path, featureId);
        Optional<NodeRef> ref = findUnstaged(featurePath);
        if (ref.isPresent()) {
            parentTree.remove(ref.get().getPath());
        }

        ObjectId newTree = repository.command(WriteBack.class).setAncestor(getTreeSupplier())
                .setChildPath(path).setToIndex(true).setTree(parentTree.build()).call();

        updateWorkHead(newTree);

        return ref.isPresent();
    }

    /**
     * Deletes a collection of features of the same type from the working tree and updates the
     * WORK_HEAD ref.
     * 
     * @param typeName feature type
     * @param filter - currently unused
     * @param affectedFeatures features to remove
     * @throws Exception
     */
    public void delete(final QName typeName, final Filter filter,
            final Iterator<Feature> affectedFeatures) throws Exception {

        RevTreeBuilder parentTree = repository.command(FindOrCreateSubtree.class)
                .setParent(Suppliers.ofInstance(Optional.of(getTree()))).setIndex(true)
                .setChildPath(typeName.getLocalPart()).call().builder(indexDatabase);

        String fid;
        String featurePath;

        while (affectedFeatures.hasNext()) {
            fid = affectedFeatures.next().getIdentifier().getID();
            featurePath = NodeRef.appendChild(typeName.getLocalPart(), fid);
            Optional<NodeRef> ref = findUnstaged(featurePath);
            if (ref.isPresent()) {
                parentTree.remove(ref.get().getPath());
            }
        }

        ObjectId newTree = repository.command(WriteBack.class)
                .setAncestor(getTree().builder(indexDatabase))
                .setChildPath(typeName.getLocalPart()).setToIndex(true).setTree(parentTree.build())
                .call();

        updateWorkHead(newTree);
    }

    /**
     * Deletes a feature type from the working tree and updates the WORK_HEAD ref.
     * 
     * @param typeName feature type to remove
     * @throws Exception
     */
    public void delete(final QName typeName) throws Exception {
        checkNotNull(typeName);

        RevTreeBuilder workRoot = getTree().builder(indexDatabase);

        final String treePath = typeName.getLocalPart();
        if (workRoot.get(treePath).isPresent()) {
            workRoot.remove(treePath);
            RevTree newRoot = workRoot.build();
            indexDatabase.put(newRoot.getId(), serialFactory.createRevTreeWriter(newRoot));
            updateWorkHead(newRoot.getId());
        }
    }

    /**
     * Insert a single feature into the working tree and updates the WORK_HEAD ref.
     * 
     * @param parentTreePath path of the parent tree to insert the feature into
     * @param feature the feature to insert
     */
    public NodeRef insert(final String parentTreePath, final Feature feature) {

        final FeatureType featureType = feature.getType();
        RevFeatureType newFeatureType = new RevFeatureType(featureType);
        ObjectId revFeatureTypeId = repository.command(HashObject.class).setObject(newFeatureType)
                .call();

        final ObjectWriter<RevFeatureType> featureTypeWriter = serialFactory
                .createFeatureTypeWriter(newFeatureType);

        indexDatabase.put(revFeatureTypeId, featureTypeWriter);

        NodeRef ref = putInDatabase(parentTreePath, feature, revFeatureTypeId);
        RevTreeBuilder parentTree = repository.command(FindOrCreateSubtree.class).setIndex(true)
                .setParent(Suppliers.ofInstance(Optional.of(getTree())))
                .setChildPath(parentTreePath).call().builder(indexDatabase);

        parentTree.put(ref);

        ObjectId newTree = repository.command(WriteBack.class).setAncestor(getTreeSupplier())
                .setChildPath(parentTreePath).setToIndex(true).setTree(parentTree.build()).call();

        updateWorkHead(newTree);
        return ref;
    }

    /**
     * Inserts a collection of features into the working tree and updates the WORK_HEAD ref.
     * 
     * @param treePath the path of the tree to insert the features into
     * @param features the features to insert
     * @param forceUseProvidedFID - currently unused
     * @param listener a {@link ProgressListener} for the current process
     * @param insertedTarget if provided, inserted features will be added to this list
     * @param collectionSize number of features to add
     * @throws Exception
     */
    public void insert(final String treePath, Iterator<Feature> features,
            boolean forceUseProvidedFID, ProgressListener listener,
            @Nullable List<NodeRef> insertedTarget, @Nullable Integer collectionSize)
            throws Exception {

        checkArgument(collectionSize == null || collectionSize.intValue() > -1);

        final Integer size = collectionSize == null || collectionSize.intValue() < 1 ? null
                : collectionSize.intValue();

        RevTreeBuilder parentTree = repository.command(FindOrCreateSubtree.class).setIndex(true)
                .setParent(Suppliers.ofInstance(Optional.of(getTree()))).setChildPath(treePath)
                .call().builder(indexDatabase);

        putInDatabase(treePath, features, listener, size, insertedTarget, parentTree);

        ObjectId newTree = repository.command(WriteBack.class).setAncestor(getTreeSupplier())
                .setChildPath(treePath).setToIndex(true).setTree(parentTree.build()).call();

        updateWorkHead(newTree);
    }

    /**
     * Updates a collection of features in the working tree and updates the WORK_HEAD ref.
     * 
     * @param treePath the path of the tree to insert the features into
     * @param features the features to insert
     * @param listener a {@link ProgressListener} for the current process
     * @param collectionSize number of features to add
     * @throws Exception
     */
    public void update(final String treePath, final Iterator<Feature> features,
            final ProgressListener listener, @Nullable final Integer collectionSize)
            throws Exception {

        checkArgument(collectionSize == null || collectionSize.intValue() > -1);

        final Integer size = collectionSize == null || collectionSize.intValue() < 1 ? null
                : collectionSize.intValue();

        insert(treePath, features, false, listener, null, size);
    }

    /**
     * Determines if a specific feature type is versioned (existing in the main repository).
     * 
     * @param typeName feature type to check
     * @return true if the feature type is versioned, false otherwise.
     */
    public boolean hasRoot(final QName typeName) {
        String localPart = typeName.getLocalPart();
        Optional<NodeRef> typeNameTreeRef = repository.command(FindTreeChild.class)
                .setChildPath(localPart).call();
        return typeNameTreeRef.isPresent();
    }

    /**
     * @param pathFilter if specified, only changes that match the filter will be returned
     * @return an iterator for all of the differences between the work tree and the index based on
     *         the path filter.
     */
    public Iterator<DiffEntry> getUnstaged(final @Nullable String pathFilter) {
        Iterator<DiffEntry> unstaged = repository.command(DiffWorkTree.class).setFilter(pathFilter)
                .call();
        return unstaged;
    }

    /**
     * @param pathFilter if specified, only changes that match the filter will be counted
     * @return the number differences between the work tree and the index based on the path filter.
     */
    public long countUnstaged(final @Nullable String pathFilter) {
        Iterator<DiffEntry> unstaged = getUnstaged(pathFilter);
        long count = 0;
        while (unstaged.hasNext()) {
            count++;
            unstaged.next();
        }
        return count;
    }

    /**
     * @param path finds a {@link NodeRef} for the feature at the given path in the index
     * @return the NodeRef for the feature at the specified path if it exists in the work tree,
     *         otherwise Optional.absent()
     */
    public Optional<NodeRef> findUnstaged(final String path) {
        Optional<NodeRef> entry = repository.command(FindTreeChild.class).setIndex(true)
                .setParent(getTree()).setChildPath(path).call();
        return entry;
    }

    /**
     * Adds a single feature to the staging database.
     * 
     * @param parentTreePath the path of the feature
     * @param feature the feature to add
     * @param metadataId
     * @return the NodeRef for the inserted feature
     */
    private NodeRef putInDatabase(final String parentTreePath, final Feature feature,
            final ObjectId metadataId) {

        NodeRef.checkValidPath(parentTreePath);
        checkNotNull(feature);
        checkNotNull(metadataId);

        final RevFeature newFeature = new RevFeatureBuilder().build(feature);
        final ObjectId objectId = repository.command(HashObject.class).setObject(newFeature).call();
        final BoundingBox bounds = feature.getBounds();
        final String nodePath = NodeRef
                .appendChild(parentTreePath, feature.getIdentifier().getID());

        final ObjectWriter<?> featureWriter = serialFactory.createFeatureWriter(newFeature);

        indexDatabase.put(objectId, featureWriter);

        NodeRef newObject;
        if (bounds == null) {
            newObject = new NodeRef(nodePath, objectId, metadataId, TYPE.FEATURE);
        } else {
            newObject = new SpatialRef(nodePath, objectId, metadataId, TYPE.FEATURE, bounds);
        }

        return newObject;
    }

    /**
     * Adds a collection of features to the staging database.
     * 
     * @param parentTreepath path of features
     * @param objects the features to insert
     * @param progress the {@link ProgressListener} for this process
     * @param size number of features to add
     * @param target if specified, created {@link NodeRef}s will be added to the list
     * @throws Exception
     */
    private void putInDatabase(final String parentTreePath, final Iterator<Feature> objects,
            final ProgressListener progress, final @Nullable Integer size,
            @Nullable final List<NodeRef> target, final RevTreeBuilder parentTree) throws Exception {

        checkNotNull(objects);
        checkNotNull(progress);
        checkNotNull(parentTree);

        Feature feature;
        int count = 0;

        progress.started();

        Map<Name, ObjectId> revFeatureTypes = Maps.newHashMap();

        while (objects.hasNext()) {
            count++;
            if (progress.isCanceled()) {
                return;
            }
            if (size != null) {
                progress.progress((float) (count * 100) / size.intValue());
            }

            feature = objects.next();

            final FeatureType featureType = feature.getType();
            ObjectId revFeatureTypeId = revFeatureTypes.get(featureType.getName());

            if (null == revFeatureTypeId) {
                RevFeatureType newFeatureType = new RevFeatureType(featureType);

                revFeatureTypeId = repository.command(HashObject.class).setObject(newFeatureType)
                        .call();

                final ObjectWriter<RevFeatureType> featureTypeWriter = serialFactory
                        .createFeatureTypeWriter(newFeatureType);

                indexDatabase.put(revFeatureTypeId, featureTypeWriter);
                revFeatureTypes.put(featureType.getName(), revFeatureTypeId);
            }

            final NodeRef objectRef = putInDatabase(parentTreePath, feature, revFeatureTypeId);
            parentTree.put(objectRef);
            if (target != null) {
                target.add(objectRef);
            }
        }

        progress.complete();
    }

    /**
     * @return a list of all the feature type names in the working tree
     */
    public List<QName> getFeatureTypeNames() {
        // List<QName> names = new ArrayList<QName>();
        RevTree root = getTree();

        final List<QName> typeNames = Lists.newLinkedList();
        if (root.children().isPresent()) {
            for (NodeRef typeTreeRef : root.children().get()) {
                String localName = NodeRef.nodeFromPath(typeTreeRef.getPath());
                typeNames.add(new QName(localName));
            }
        }
        return typeNames;
    }
}
