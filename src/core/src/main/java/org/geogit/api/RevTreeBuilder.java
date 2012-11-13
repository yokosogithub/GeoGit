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
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.HashObject;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.NodeRefPathStorageOrder;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

public class RevTreeBuilder {

    /**
     * How many children refs to hold before forcing normalization of the internal data structures
     * into tree buckets on the database
     * 
     * @todo make this configurable
     */
    public static final int DEFAULT_NORMALIZATION_THRESHOLD = 1000 * 10;

    private final ObjectDatabase db;

    protected final ObjectSerialisingFactory serialFactory;

    private final TreeMultimap<Integer, NodeRef> entriesByBucket;

    protected final TreeMap<Integer, ObjectId> bucketTreesByBucket;

    private int depth;

    protected NodeRefPathStorageOrder storageOrder = new NodeRefPathStorageOrder();

    private long size;

    /**
     * Empty tree constructor, used to create trees from scratch
     * 
     * @param db
     * @param serialFactory
     */
    public RevTreeBuilder(ObjectDatabase db, final ObjectSerialisingFactory serialFactory) {
        this(db, serialFactory, null);
    }

    public long size() {
        return size;
    }

    /**
     * Copy constructor with tree depth
     */
    public RevTreeBuilder(ObjectDatabase db, final ObjectSerialisingFactory serialFactory,
            @Nullable final RevTree copy) {
        this(db, serialFactory, copy, 0);
    }

    /**
     * Copy constructor
     */
    public RevTreeBuilder(ObjectDatabase db, final ObjectSerialisingFactory serialFactory,
            @Nullable final RevTree copy, int depth) {

        checkNotNull(db);
        checkNotNull(serialFactory);
        this.db = db;
        this.depth = depth;
        this.serialFactory = serialFactory;

        Comparator<Integer> keyComparator = Ordering.natural();
        Comparator<NodeRef> valueComparator = new Comparator<NodeRef>() {
            @Override
            public int compare(NodeRef o1, NodeRef o2) {
                return o1.getPath().compareTo(o2.getPath());
            }
        };
        this.entriesByBucket = TreeMultimap.create(keyComparator, valueComparator);
        this.bucketTreesByBucket = Maps.newTreeMap();

        if (copy != null) {
            this.size = copy.size();
            if (copy.children().isPresent()) {
                checkArgument(!copy.buckets().isPresent());
                for (NodeRef ref : copy.children().get()) {
                    String path = ref.getPath();
                    Integer bucket = computeBucket(path);
                    entriesByBucket.put(bucket, ref);
                }
            }
            if (copy.buckets().isPresent()) {
                checkArgument(!copy.children().isPresent());
                bucketTreesByBucket.putAll(copy.buckets().get());
            }
        }
    }

    /**
     * @return the new tree, not saved to the object database. Any bucket tree thouh is saved when
     *         this method returns.
     */
    public RevTree build() {
        normalize();
        checkState(this.bucketTreesByBucket.isEmpty() || this.entriesByBucket.isEmpty());

        RevTreeImpl unnamedTree;

        if (!bucketTreesByBucket.isEmpty()) {
            unnamedTree = RevTreeImpl.createNodeTree(ObjectId.NULL, size, bucketTreesByBucket);
        } else {
            Collection<NodeRef> entries = Collections2.filter(entriesByBucket.values(),
                    new Predicate<NodeRef>() {
                        @Override
                        public boolean apply(NodeRef input) {
                            ObjectId objectId = input.getObjectId();
                            boolean applies = !ObjectId.NULL.equals(objectId);
                            return applies;
                        }
                    });
            unnamedTree = RevTreeImpl.createLeafTree(ObjectId.NULL, size, entries);
        }

        ObjectId treeId = new HashObject().setObject(unnamedTree).call();
        RevTreeImpl namedTree = RevTreeImpl.create(treeId, size, unnamedTree);
        return namedTree;
    }

    /**
     * Gets an entry by key, this is potentially slow.
     * 
     * @param key
     * @return
     */
    public Optional<NodeRef> get(final String key) {

        final Integer bucket = computeBucket(key);

        return getInternal(key, bucket, true);
    }

    private Optional<NodeRef> getInternal(final String key, final Integer bucket, final boolean deep) {
        if (entriesByBucket.containsKey(bucket)) {
            NodeRef found = null;
            SortedSet<NodeRef> bucketEntries = entriesByBucket.get(bucket);
            for (NodeRef ref : bucketEntries) {
                if (ref.getPath().equals(key)) {
                    if (found != null) {
                        throw new IllegalStateException("Already found: " + found);
                    }
                    found = ref;
                }
            }
            if (found != null) {
                if (ObjectId.NULL.equals(found.getObjectId())) {
                    return Optional.absent();
                } else {
                    return Optional.of(found);
                }
            }
        }
        if (!deep) {
            return Optional.absent();
        }
        final ObjectId subtreeId = bucketTreesByBucket.get(bucket);
        if (subtreeId == null) {
            return Optional.absent();
        }

        ObjectReader<RevTree> reader = serialFactory.createRevTreeReader();
        RevTree subtree = db.get(subtreeId, reader);

        DepthSearch depthSearch = new DepthSearch(db, serialFactory);
        Optional<NodeRef> ref = depthSearch.getDirectChild(subtree, key, depth + 1);
        return ref;
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
    public RevTreeBuilder put(final NodeRef ref) {
        Preconditions.checkNotNull(ref, "ref can't be null");

        final Integer bucket = computeBucket(ref.getPath());

        final Optional<NodeRef> existing = this.getInternal(ref.getPath(), bucket, false);
        if (existing.isPresent()) {
            this.entriesByBucket.remove(bucket, existing.get());
        }
        entriesByBucket.put(bucket, ref);
        if (ref.getType().equals(RevObject.TYPE.FEATURE)) {
            if (!existing.isPresent()) {
                size += 1;
            }
        } else if (ref.getType().equals(RevObject.TYPE.TREE)) {
            long oldChildSize = existing.isPresent() ? sizeOfTree(existing.get().getObjectId())
                    : 0L;
            long newChildSize = sizeOfTree(ref.getObjectId());
            this.size += (newChildSize - oldChildSize);
        }
        if (entriesByBucket.size() >= DEFAULT_NORMALIZATION_THRESHOLD) {
            // hit the split factor modification tolerance, lets normalize
            normalize();
        }
        return this;
    }

    private long sizeOfTree(ObjectId treeId) {
        if (treeId.isNull()) {
            return 0L;
        }
        RevTree tree = db.get(treeId, serialFactory.createRevTreeReader());
        return tree.size();
    }

    public RevTreeBuilder remove(final String childPath) {
        Preconditions.checkNotNull(childPath, "key can't be null");
        // uses a NodeRef with ObjectId.NULL id signaling the removal
        // of the entry. normalize() is gonna take care of removing it from the subtree
        // subsequently

        final Integer bucket = computeBucket(childPath);

        Optional<NodeRef> ref = this.getInternal(childPath, bucket, true);
        if (ref.isPresent()) {
            NodeRef child = ref.get();
            entriesByBucket.remove(bucket, child);

            entriesByBucket.put(bucket, new NodeRef(childPath, ObjectId.NULL, ObjectId.NULL,
                    TYPE.FEATURE));
            if (TYPE.FEATURE.equals(child.getType())) {
                this.size--;
            } else {
                checkState(TYPE.TREE.equals(child.getType()));
                this.size -= sizeOfTree(child.getObjectId());
            }
            if (entriesByBucket.size() >= DEFAULT_NORMALIZATION_THRESHOLD) {
                normalize();
            }
        }

        return this;
    }

    public boolean isNormalized() {

        final boolean empty = entriesByBucket.isEmpty() && bucketTreesByBucket.isEmpty();

        final boolean childrenBetweenLimitsAndNoBuckets = entriesByBucket.size() <= NORMALIZED_SIZE_LIMIT
                && bucketTreesByBucket.isEmpty();

        boolean onlyBuckets = entriesByBucket.isEmpty() && !bucketTreesByBucket.isEmpty();

        boolean normalized = empty || childrenBetweenLimitsAndNoBuckets || onlyBuckets;
        return normalized;
    }

    /**
     * Splits the cached entries into subtrees and saves them, making sure the tree contains either
     * only entries or subtrees
     */
    public void normalize() {
        if (isNormalized()) {
            return;
        }

        // update all inner trees
        final int childDepth = this.depth + 1;
        try {
            for (Integer bucket : entriesByBucket.keySet()) {
                final RevTreeBuilder subtreeBuilder;
                {
                    final SortedSet<NodeRef> bucketEntries = entriesByBucket.get(bucket);
                    final ObjectId subtreeId = bucketTreesByBucket.get(bucket);
                    if (subtreeId == null) {
                        subtreeBuilder = new RevTreeBuilder(db, serialFactory, null, childDepth);
                    } else {
                        ObjectReader<RevTree> reader = serialFactory.createRevTreeReader();
                        subtreeBuilder = new RevTreeBuilder(db, serialFactory, db.get(subtreeId,
                                reader), childDepth);
                    }
                    for (NodeRef ref : bucketEntries) {
                        if (ObjectId.NULL.equals(ref.getObjectId())) {
                            subtreeBuilder.remove(ref.getPath());
                        } else {
                            subtreeBuilder.put(ref);
                        }
                    }
                }
                final RevTree subtree = subtreeBuilder.build();
                if (subtree.isEmpty()) {
                    bucketTreesByBucket.remove(bucket);
                } else {
                    final ObjectId newSubtreeId = subtree.getId();
                    db.put(newSubtreeId, serialFactory.createRevTreeWriter(subtree));
                    bucketTreesByBucket.put(bucket, newSubtreeId);
                }
            }
            entriesByBucket.clear();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // did we reach back the upper limit for a children tree? then compress to a children tree
        if (size <= NORMALIZED_SIZE_LIMIT) {

            final long oldSize = this.size;
            this.size = 0L;

            ImmutableList<ObjectId> bucketIds = ImmutableList.copyOf(bucketTreesByBucket.values());
            bucketTreesByBucket.clear();

            for (ObjectId bucketId : bucketIds) {
                ObjectReader<RevTree> reader = serialFactory.createRevTreeReader();
                RevTree bucket = db.get(bucketId, reader);
                checkState(bucket.children().isPresent());
                for (NodeRef ref : bucket.children().get()) {
                    put(ref);
                }
            }
            checkState(size == oldSize);
        }
    }

    protected final Integer computeBucket(final String path) {
        return this.storageOrder.bucket(path, this.depth);
    }

}
