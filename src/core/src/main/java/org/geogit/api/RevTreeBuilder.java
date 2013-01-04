/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.geogit.api.RevTree.NORMALIZED_SIZE_LIMIT;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.HashObject;
import org.geogit.api.plumbing.diff.DepthTreeIterator;
import org.geogit.api.plumbing.diff.DepthTreeIterator.Strategy;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.NodePathStorageOrder;
import org.geogit.storage.ObjectDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class RevTreeBuilder {

    /**
     * How many children nodes to hold before forcing normalization of the internal data structures
     * into tree buckets on the database
     * 
     * @todo make this configurable
     */
    public static final int DEFAULT_NORMALIZATION_THRESHOLD = 1000 * 100;

    private final ObjectDatabase db;

    private final Set<String> deletes;

    private final Map<String, Node> treeChanges;

    private final Map<String, Node> featureChanges;

    protected final TreeMap<Integer, ObjectId> bucketTreesByBucket;

    private int depth;

    protected NodePathStorageOrder storageOrder = new NodePathStorageOrder();

    /**
     * Empty tree constructor, used to create trees from scratch
     * 
     * @param db
     * @param serialFactory
     */
    public RevTreeBuilder(ObjectDatabase db) {
        this(db, null);
    }

    /**
     * Copy constructor with tree depth
     */
    public RevTreeBuilder(ObjectDatabase db, @Nullable final RevTree copy) {
        this(db, copy, 0);
    }

    /**
     * Copy constructor
     */
    public RevTreeBuilder(ObjectDatabase db, @Nullable final RevTree copy, int depth) {

        checkNotNull(db);
        this.db = db;
        this.depth = depth;

        this.deletes = Sets.newHashSet();
        this.treeChanges = Maps.newHashMap();
        this.featureChanges = Maps.newHashMap();
        this.bucketTreesByBucket = Maps.newTreeMap();

        if (copy != null) {
            if (copy.trees().isPresent()) {
                checkArgument(!copy.buckets().isPresent());
                for (Node node : copy.trees().get()) {
                    putInternal(node);
                }
            }
            if (copy.features().isPresent()) {
                checkArgument(!copy.buckets().isPresent());
                for (Node node : copy.features().get()) {
                    putInternal(node);
                }
            }
            if (copy.buckets().isPresent()) {
                checkArgument(!copy.features().isPresent());
                bucketTreesByBucket.putAll(copy.buckets().get());
            }
        }
    }

    /**
     * @param bucket
     * @param node
     * @return
     */
    private @Nullable
    Node putInternal(Node node) {
        switch (node.getType()) {
        case FEATURE:
            return featureChanges.put(node.getName(), node);
        case TREE:
            return treeChanges.put(node.getName(), node);
        default:
            throw new IllegalArgumentException(
                    "Only tree or feature nodes can be added to a tree: " + node + " "
                            + node.getType());
        }
    }

    private RevTree loadTree(final ObjectId subtreeId) {
        RevTree subtree = db.getTree(subtreeId);
        return subtree;
    }

    private Optional<Node> getInternal(final String key, final boolean deep) {
        Node found = featureChanges.get(key);
        if (found == null) {
            found = treeChanges.get(key);
        }
        if (found != null) {
            return Optional.of(found);
        }

        if (!deep) {
            return Optional.absent();
        }
        if (deletes.contains(key)) {
            return Optional.absent();
        }

        final Integer bucket = computeBucket(key);
        final ObjectId subtreeId = bucketTreesByBucket.get(bucket);
        if (subtreeId == null) {
            return Optional.absent();
        }

        RevTree subtree = loadTree(subtreeId);

        DepthSearch depthSearch = new DepthSearch(db);
        Optional<Node> node = depthSearch.getDirectChild(subtree, key, depth + 1);

        if (node.isPresent()) {
            return Optional.of(node.get());
        } else {
            return Optional.absent();
        }
    }

    private long sizeOfTree(ObjectId treeId) {
        if (treeId.isNull()) {
            return 0L;
        }
        RevTree tree = loadTree(treeId);
        return tree.size();
    }

    private int numPendingChanges() {
        int totalChanges = featureChanges.size() + treeChanges.size() + deletes.size();
        return totalChanges;
    }

    /**
     * Splits the cached entries into subtrees and saves them, making sure the tree contains either
     * only entries or subtrees
     */
    private RevTree normalize() {
        RevTree unnamedTree;

        if (bucketTreesByBucket.isEmpty() && numPendingChanges() <= NORMALIZED_SIZE_LIMIT) {
            unnamedTree = normalizeToChildren();
        } else {
            unnamedTree = normalizeToBuckets();
            if (unnamedTree.isEmpty()) {
                return RevTree.EMPTY;
            }
            if (unnamedTree.size() <= NORMALIZED_SIZE_LIMIT) {
                unnamedTree = moveBucketsToChildren(unnamedTree);
            }
        }
        return unnamedTree;
    }

    /**
     * @param unnamedTree
     * @return
     */
    private RevTree moveBucketsToChildren(RevTree unnamedTree) {
        checkState(featureChanges.isEmpty());
        // TODO:**********
        Iterator<NodeRef> iterator = new DepthTreeIterator("", ObjectId.NULL, unnamedTree, db,
                Strategy.CHILDREN);
        this.bucketTreesByBucket.clear();
        while (iterator.hasNext()) {
            put(iterator.next().getNode());
        }
        return normalizeToChildren();
    }

    /**
     * 
     */
    private RevTree normalizeToChildren() {
        Preconditions.checkState(this.bucketTreesByBucket.isEmpty());
        // remove deletes
        deletes.clear();

        long size = featureChanges.size();
        if (!treeChanges.isEmpty()) {
            for (Node node : treeChanges.values()) {
                size += sizeOf(node);
            }
        }
        Collection<Node> features = featureChanges.values();
        Collection<Node> trees = treeChanges.values();
        RevTreeImpl unnamedTree = RevTreeImpl.createLeafTree(ObjectId.NULL, size, features, trees);
        return unnamedTree;
    }

    private long sizeOf(Node node) {
        return node.getType().equals(TYPE.TREE) ? sizeOfTree(node.getObjectId()) : 1L;
    }

    /**
     * @return
     * 
     */
    private RevTree normalizeToBuckets() {
        // update all inner trees
        final int childDepth = this.depth + 1;
        long accSize = 0;
        int accChildTreeCount = 0;
        try {
            Multimap<Integer, Node> changesByBucket = ArrayListMultimap.create();
            for (Iterator<Node> it = featureChanges.values().iterator(); it.hasNext();) {
                Node change = it.next();
                it.remove();
                Integer bucket = computeBucket(change.getName());
                changesByBucket.put(bucket, change);
            }
            Preconditions.checkState(featureChanges.isEmpty());
            for (Iterator<Node> it = treeChanges.values().iterator(); it.hasNext();) {
                Node change = it.next();
                it.remove();
                Integer bucket = computeBucket(change.getName());
                changesByBucket.put(bucket, change);
            }
            Preconditions.checkState(featureChanges.isEmpty());

            final Set<Integer> buckets = ImmutableSet.copyOf(Sets.union(changesByBucket.keySet(),
                    bucketTreesByBucket.keySet()));

            for (Integer bucket : buckets) {
                final RevTreeBuilder bucketTreeBuilder;
                {
                    final Collection<Node> bucketEntries = changesByBucket.removeAll(bucket);
                    final ObjectId subtreeId = bucketTreesByBucket.get(bucket);
                    if (subtreeId == null) {
                        bucketTreeBuilder = new RevTreeBuilder(db, null, childDepth);
                    } else {
                        bucketTreeBuilder = new RevTreeBuilder(db, loadTree(subtreeId), childDepth);
                    }
                    for (String deleted : deletes) {
                        Integer bucketOfDelete = computeBucket(deleted);
                        if (bucket.equals(bucketOfDelete)) {
                            bucketTreeBuilder.remove(deleted);
                        }
                    }
                    for (Node node : bucketEntries) {
                        bucketTreeBuilder.put(node);
                    }
                }
                final RevTree bucketTree = bucketTreeBuilder.build();
                accSize += bucketTree.size();
                accChildTreeCount += bucketTree.numTrees();
                if (bucketTree.isEmpty()) {
                    bucketTreesByBucket.remove(bucket);
                } else {
                    db.put(bucketTree);
                    bucketTreesByBucket.put(bucket, bucketTree.getId());
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return RevTreeImpl.createNodeTree(ObjectId.NULL, accSize, accChildTreeCount,
                this.bucketTreesByBucket);
    }

    protected final Integer computeBucket(final String path) {
        return this.storageOrder.bucket(path, this.depth);
    }

    /**
     * Gets an entry by key, this is potentially slow.
     * 
     * @param key
     * @return
     */
    public Optional<Node> get(final String key) {
        return getInternal(key, true);
    }

    /**
     * Adds or replaces an element in the tree with the given key.
     * <p>
     * <!-- Implementation detail: If the number of cached entries (entries held directly by this
     * tree) reaches {@link #DEFAULT_NORMALIZATION_THRESHOLD}, this tree will {@link #normalize()}
     * itself.
     * 
     * -->
     * 
     * @param key non null
     * @param value non null
     */
    public RevTreeBuilder put(final Node node) {
        Preconditions.checkNotNull(node, "node can't be null");

        putInternal(node);
        if (numPendingChanges() >= DEFAULT_NORMALIZATION_THRESHOLD) {
            // hit the split factor modification tolerance, lets normalize
            normalize();
        }
        return this;
    }

    /**
     * Removes an element from the tree
     * 
     * @param childName the name of the child to remove
     * @return {@code this}
     */
    public RevTreeBuilder remove(final String childName) {
        Preconditions.checkNotNull(childName, "key can't be null");
        // uses a Node with ObjectId.NULL id signaling the removal
        // of the entry. normalize() is gonna take care of removing it from the subtree
        // subsequently
        featureChanges.remove(childName);
        treeChanges.remove(childName);
        deletes.add(childName);
        return this;
    }

    /**
     * @return the new tree, not saved to the object database. Any bucket tree though is saved when
     *         this method returns.
     */
    public RevTree build() {
        RevTree unnamedTree = normalize();
        checkState(bucketTreesByBucket.isEmpty()
                || (featureChanges.isEmpty() && treeChanges.isEmpty()));

        ObjectId treeId = new HashObject().setObject(unnamedTree).call();
        RevTreeImpl namedTree = RevTreeImpl.create(treeId, unnamedTree.size(), unnamedTree);
        return namedTree;
    }
}
