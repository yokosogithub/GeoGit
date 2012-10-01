/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.StagingDatabase;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
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
 * @param the actual type representing a Feature
 * @param the actual type representing a FeatureType
 * @author Gabriel Roldan
 * @see Repository
 */
public class WorkingTree {

    @Inject
    private StagingArea index;

    @Inject
    private Repository repository;

    // public NodeRef init(final RevFeatureType featureType) throws Exception {
    //
    // final QName typeName = featureType.getName();
    // final String path = typeName.getLocalPart();
    //
    // ObjectWriter<RevFeatureType> typeWriter;
    // typeWriter = repository.newFeatureTypeWriter(featureType);
    // ObjectId metadataId = index.getDatabase().put(typeWriter);
    // NodeRef treeRef = index.created(path, metadataId);
    // checkState(treeRef != null);
    // return treeRef;
    // }

    public void delete(final QName typeName) throws Exception {
        index.deleted(typeName.getLocalPart());
    }

    private String path(final QName typeName, final String id) {
        String path = typeName.getLocalPart();
        if (id != null) {
            path = NodeRef.appendChild(path, id);
        }
        return path;
    }

    /**
     * @param features
     * @param forceUseProvidedFIDs
     * @param nullProgressListener
     * @param inserted
     * @throws Exception
     */
    public void insert(final String treePath, Iterator<RevFeature> features,
            boolean forceUseProvidedFID, ProgressListener listener,
            @Nullable List<NodeRef> insertedTarget, @Nullable Integer collectionSize)
            throws Exception {

        checkArgument(collectionSize == null || collectionSize.intValue() > -1);

        final Integer size = collectionSize == null || collectionSize.intValue() < 1 ? null
                : collectionSize.intValue();

        index.insert(treePath, features, listener, size, insertedTarget);
    }

    public void update(final String treePath, final Iterator<RevFeature> features,
            final ProgressListener listener, @Nullable final Integer collectionSize)
            throws Exception {

        checkArgument(collectionSize == null || collectionSize.intValue() > -1);
        final int size = collectionSize == null ? -1 : collectionSize.intValue();

        index.insert(treePath, features, listener, size, null);
    }

    public boolean hasRoot(final QName typeName) {
        String localPart = typeName.getLocalPart();
        Optional<NodeRef> typeNameTreeRef = repository.command(FindTreeChild.class)
                .setChildPath(localPart).call();
        return typeNameTreeRef.isPresent();
    }

    public void delete(final QName typeName, final Filter filter,
            final Iterator<RevFeature> affectedFeatures) throws Exception {

        final StagingArea index = repository.getIndex();

        String fid;
        String featurePath;

        while (affectedFeatures.hasNext()) {
            fid = affectedFeatures.next().getFeatureId();
            featurePath = path(typeName, fid);
            index.deleted(featurePath);
        }
    }

    /**
     * @return
     */
    public List<QName> getFeatureTypeNames() {
        List<QName> names = new ArrayList<QName>();
        RevTree root = repository.getOrCreateHeadTree();

        final List<QName> typeNames = Lists.newLinkedList();
        if (root != null) {
            root.accept(new TreeVisitor() {

                @Override
                public boolean visitSubTree(int bucket, ObjectId treeId) {
                    return false;
                }

                @Override
                public boolean visitEntry(NodeRef ref) {
                    if (TYPE.TREE.equals(ref.getType())) {
                        if (!ref.getMetadataId().isNull()) {
                            ObjectId metadataId = ref.getMetadataId();
                            ObjectSerialisingFactory serialFactory;
                            serialFactory = repository.getSerializationFactory();
                            ObjectReader<RevFeatureType> typeReader = serialFactory
                                    .createFeatureTypeReader();
                            StagingDatabase database = index.getDatabase();
                            RevFeatureType type = database.get(metadataId, typeReader);
                            typeNames.add(type.getName());
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }
        return names;
    }

    // public RevTree getHeadVersion(final QName typeName) {
    // final String featureTreePath = path(typeName, null);
    // Optional<NodeRef> typeTreeRef = repository.getRootTreeChild(featureTreePath);
    // RevTree typeTree;
    // if (typeTreeRef.isPresent()) {
    // typeTree = repository.getTree(typeTreeRef.get().getObjectId());
    // } else {
    // typeTree = repository.newTree();
    // }
    // return typeTree;
    // }
    //
    // public RevTree getStagedVersion(final QName typeName) {
    //
    // RevTree typeTree = getHeadVersion(typeName);
    //
    // String path = path(typeName, null);
    // StagingDatabase database = index.getDatabase();
    // final int stagedCount = database.countStaged(path);
    // if (stagedCount == 0) {
    // return typeTree;
    // }
    // return new DiffTree(typeTree, path, index);
    // }
    //
    // private static class DiffTree implements RevTree {
    //
    // private final RevTree typeTree;
    //
    // private final Map<String, NodeRef> inserts = new HashMap<String, NodeRef>();
    //
    // private final Map<String, NodeRef> updates = new HashMap<String, NodeRef>();
    //
    // private final Set<String> deletes = new HashSet<String>();
    //
    // public DiffTree(final RevTree typeTree, final String basePath, final StagingArea index) {
    // this.typeTree = typeTree;
    //
    // Iterator<NodeRef> staged = index.getDatabase().getStaged(basePath);
    // NodeRef entry;
    // String fid;
    // while (staged.hasNext()) {
    // entry = staged.next();
    // switch (entry.changeType()) {
    // case ADDED:
    // fid = fid(entry.newPath());
    // inserts.put(fid, entry.getNewObject());
    // break;
    // case REMOVED:
    // fid = fid(entry.oldPath());
    // deletes.add(fid);
    // break;
    // case MODIFIED:
    // fid = fid(entry.newPath());
    // updates.put(fid, entry.getNewObject());
    // break;
    // default:
    // throw new IllegalStateException();
    // }
    // }
    // }
    //
    // /**
    // * Extracts the feature id (last path step) from a full path
    // */
    // private String fid(String featurePath) {
    // int idx = featurePath.lastIndexOf(NodeRef.PATH_SEPARATOR);
    // return featurePath.substring(idx);
    // }
    //
    // @Override
    // public TYPE getType() {
    // return TYPE.TREE;
    // }
    //
    // @Override
    // public ObjectId getId() {
    // return null;
    // }
    //
    // @Override
    // public boolean isNormalized() {
    // return false;
    // }
    //
    // @Override
    // public MutableTree mutable() {
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public Optional<NodeRef> get(final String fid) {
    // NodeRef ref = inserts.get(fid);
    // if (ref == null) {
    // ref = updates.get(fid);
    // if (ref == null) {
    // return this.typeTree.get(fid);
    // }
    // }
    // return Optional.of(ref);
    // }
    //
    // @Override
    // public void accept(TreeVisitor visitor) {
    // throw new UnsupportedOperationException();
    // }
    //
    // @Override
    // public BigInteger size() {
    // BigInteger size = typeTree.size();
    // if (inserts.size() > 0) {
    // size = size.add(BigInteger.valueOf(inserts.size()));
    // }
    // if (deletes.size() > 0) {
    // size = size.subtract(BigInteger.valueOf(deletes.size()));
    // }
    // return size;
    // }
    //
    // @Override
    // public Iterator<NodeRef> iterator(Predicate<NodeRef> filter) {
    // Iterator<NodeRef> current = typeTree.iterator(null);
    //
    // current = Iterators.filter(current, new Predicate<NodeRef>() {
    // @Override
    // public boolean apply(NodeRef input) {
    // boolean returnIt = !deletes.contains(input.getPath());
    // return returnIt;
    // }
    // });
    // current = Iterators.transform(current, new Function<NodeRef, NodeRef>() {
    // @Override
    // public NodeRef apply(NodeRef input) {
    // NodeRef update = updates.get(input.getPath());
    // return update == null ? input : update;
    // }
    // });
    //
    // Iterator<NodeRef> inserted = inserts.values().iterator();
    // if (filter != null) {
    // inserted = Iterators.filter(inserted, filter);
    // current = Iterators.filter(current, filter);
    // }
    //
    // Iterator<NodeRef> diffed = Iterators.concat(inserted, current);
    // return diffed;
    // }
    //
    // }
}
