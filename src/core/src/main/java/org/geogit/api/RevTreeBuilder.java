/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.geogit.api.RevTree.NORMALIZED_SIZE_LIMIT;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.HashObject;
import org.geogit.repository.DepthSearch;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

public class RevTreeBuilder {

    /**
     * How many children to hold before splitting myself into subtrees
     * 
     * @todo make this configurable
     */
    public static int DEFAULT_SPLIT_FACTOR = 1000 * 100;

    private final ObjectDatabase db;

    protected final ObjectSerialisingFactory serialFactory;

    private final TreeMultimap<Integer, NodeRef> entriesByBucket;

    protected final TreeMap<Integer, ObjectId> innerTrees;

    private int depth;

    protected MessageDigest md;

    /**
     * Empty tree constructor, used to create trees from scratch
     * 
     * @param db
     * @param serialFactory
     */
    public RevTreeBuilder(ObjectDatabase db, final ObjectSerialisingFactory serialFactory) {
        this(db, serialFactory, null);
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
        this.innerTrees = Maps.newTreeMap();

        if (copy != null) {
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
                innerTrees.putAll(copy.buckets().get());
            }
        }
    }

    public RevTree build() {
        normalize();
        checkState(this.innerTrees.isEmpty() || this.entriesByBucket.isEmpty());

        RevTreeImpl unnamedTree;

        if (!innerTrees.isEmpty()) {
            unnamedTree = RevTreeImpl.createNodeTree(ObjectId.NULL, innerTrees);
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
            unnamedTree = RevTreeImpl.createLeafTree(ObjectId.NULL, entries);
        }

        ObjectId treeId = new HashObject().setObject(unnamedTree).call();
        RevTreeImpl namedTree = RevTreeImpl.create(treeId, unnamedTree);
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
        final ObjectId subtreeId = innerTrees.get(bucket);
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
     * tree) reaches {@link #DEFAULT_SPLIT_FACTOR}, this tree will {@link #normalize()} itself.
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
        if (entriesByBucket.size() >= DEFAULT_SPLIT_FACTOR) {
            // hit the split factor modification tolerance, lets normalize
            normalize();
        }
        return this;
    }

    public RevTreeBuilder remove(final String childPath) {
        Preconditions.checkNotNull(childPath, "key can't be null");
        // uses a NodeRef with ObjectId.NULL id signaling the removal
        // of the entry. normalize() is gonna take care of removing it from the subtree
        // subsequently

        final Integer bucket = computeBucket(childPath);

        Optional<NodeRef> ref = this.getInternal(childPath, bucket, true);
        if (ref.isPresent()) {
            entriesByBucket.remove(bucket, ref.get());

            entriesByBucket.put(bucket, new NodeRef(childPath, ObjectId.NULL, ObjectId.NULL,
                    TYPE.FEATURE));
            if (entriesByBucket.size() >= DEFAULT_SPLIT_FACTOR) {
                normalize();
            }
        }

        return this;
    }

    public boolean isNormalized() {
        boolean normalized = (entriesByBucket.isEmpty() && innerTrees.isEmpty())
                || (entriesByBucket.size() <= NORMALIZED_SIZE_LIMIT && innerTrees.isEmpty())
                || (entriesByBucket.isEmpty() && !innerTrees.isEmpty());
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

        final int childDepth = this.depth + 1;
        try {
            for (Integer bucket : entriesByBucket.keySet()) {
                final RevTreeBuilder subtreeBuilder;
                {
                    final SortedSet<NodeRef> bucketEntries = entriesByBucket.get(bucket);
                    final ObjectId subtreeId = innerTrees.get(bucket);
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
                final ObjectId newSubtreeId = subtree.getId();
                db.put(newSubtreeId, serialFactory.createRevTreeWriter(subtree));
                innerTrees.put(bucket, newSubtreeId);
            }
            entriesByBucket.clear();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final Integer computeBucket(final String key) {
        byte[] hashedKey = hashKey(key);
        // int ch1 = hashedKey[2 * this.order] & 0xFF;
        // int ch2 = hashedKey[2 * this.order + 1] & 0xFF;
        // int b = (ch1 << 8) + (ch2 << 0);
        // final Integer bucket = Integer.valueOf(b);
        final Integer bucket = Integer.valueOf(hashedKey[this.depth] & 0xFF);

        return bucket;
    }

    private synchronized byte[] hashKey(final String key) {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("SHA1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            md.reset();
            return md.digest(key.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
