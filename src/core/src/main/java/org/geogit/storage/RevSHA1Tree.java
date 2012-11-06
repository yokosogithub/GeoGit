/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.geogit.api.AbstractRevObject;
import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;

/**
 * Provides an implementation of {@link RevTree}.
 * 
 * @see RevTree
 */
public class RevSHA1Tree extends AbstractRevObject implements RevTree {

    /**
     * How many children to hold before splitting myself into subtrees.
     */
    // TODO: make this configurable
    public static final int SPLIT_FACTOR = 64 * 1024;

    /**
     * The canonical max size of a tree, hard limit, can't be changed or would affect the hash of
     * trees
     * 
     * @todo evaluate what a good compromise would be re memory usagge/speed
     */
    protected static final int NORMALIZED_SIZE_LIMIT = 4 * 1024;

    protected final int depth;

    protected final ObjectDatabase db;

    protected MessageDigest md;

    // aggregated number of leaf nodes (data entries)
    // private final BigInteger size;

    /**
     * If split == true, holds references to other trees, if split == false, holds references to
     * data elements
     */
    protected final TreeMap<String, NodeRef> myEntries;

    protected final TreeMap<Integer, ObjectId> mySubTrees;

    protected final ObjectSerialisingFactory serialFactory;

    /**
     * Constructs a new {@code RevSHA1Tree} with the provided parameters.
     * 
     * @param db the object database
     * @param serialFactory the serialization factory
     */
    public RevSHA1Tree(final ObjectDatabase db, final ObjectSerialisingFactory serialFactory) {
        this(null, db, 0, serialFactory);
    }

    /**
     * Constructs a new {@code RevSHA1Tree} with the provided parameters.
     * 
     * @param db the object database
     * @param order the depth
     * @param serialFactory the serialization factory
     */
    RevSHA1Tree(final ObjectDatabase db, final int order,
            final ObjectSerialisingFactory serialFactory) {
        this(null, db, order, serialFactory);
    }

    /**
     * Constructs a new {@code RevSHA1Tree} with the provided parameters.
     * 
     * @param id the object id to use
     * @param db the object database
     * @param order the depth
     * @param serialFactory the serialization factory
     */
    public RevSHA1Tree(final ObjectId id, final ObjectDatabase db, final int order,
            final ObjectSerialisingFactory serialFactory) {
        this(id, db, order, new TreeMap<String, NodeRef>(), new TreeMap<Integer, ObjectId>(),
                BigInteger.ZERO, serialFactory);
    }

    /**
     * Constructs a new {@code RevSHA1Tree} with the provided parameters.
     * 
     * @param id the object id to use
     * @param db the object database
     * @param order the depth
     * @param references {@link NodeRef nodes} and their paths
     * @param subTrees subtree ids and their indices
     * @param size unused
     * @param serialFactory the serialization factory
     */
    public RevSHA1Tree(final ObjectId id, final ObjectDatabase db, final int order,
            TreeMap<String, NodeRef> references, TreeMap<Integer, ObjectId> subTrees,
            final BigInteger size, final ObjectSerialisingFactory serialFactory) {

        super(id, TYPE.TREE);
        this.db = db;
        this.depth = order;
        this.myEntries = references;
        this.mySubTrees = subTrees;
        // this.size = size;
        this.serialFactory = serialFactory;
    }

    /**
     * @return a mutable version of this tree.
     */
    @Override
    public MutableTree mutable() {
        return new MutableRevSHA1Tree(this, serialFactory);
    }

    // /**
    // * @return the number of elements in the tree
    // */
    // @Override
    // public BigInteger size() {
    // return size;
    // }

    /**
     * Accepts the provided tree visitor, providing a way to perform various actions on leaves and
     * subtrees of this tree.
     * 
     * @param visitor the visitor to use
     */
    @Override
    @SuppressWarnings("unchecked")
    public void accept(TreeVisitor visitor) {
        // first visit all the cached entries
        accept(visitor, Collections.EMPTY_MAP);

        // and then the ones in the stored subtrees
        if (mySubTrees.size() > 0) {
            final int childDepth = this.depth + 1;
            Integer bucket;
            ObjectId subtreeId;
            RevSHA1Tree subtree;

            for (Entry<Integer, ObjectId> e : mySubTrees.entrySet()) {
                bucket = e.getKey();
                subtreeId = e.getValue();
                if (visitor.visitSubTree(bucket, subtreeId)) {
                    subtree = (RevSHA1Tree) db.get(subtreeId,
                            serialFactory.createRevTreeReader(db, childDepth));
                    subtree.accept(visitor, myEntries);
                }
            }
        }
    }

    private void accept(final TreeVisitor visitor, final Map<String, NodeRef> ignore) {
        if (myEntries.size() > 0) {
            for (Map.Entry<String, NodeRef> e : myEntries.entrySet()) {
                String key = e.getKey();
                if (ignore.containsKey(key)) {
                    continue;
                }
                NodeRef value = e.getValue();
                if (!visitor.visitEntry(value)) {
                    return;
                }
            }
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

    /**
     * Retrieves the {@link NodeRef} that matches the given key from the tree, this is potentially
     * slow.
     * 
     * @param key the path of the node to get
     * @return an {@link Optional} of the node if it exists, or {@link Optional#absent()} if it
     *         wasn't found.
     */
    @Override
    public Optional<NodeRef> get(final String key) {
        NodeRef value = null;
        if (myEntries.containsKey(key)) {
            value = myEntries.get(key);
            if (value == null) {
                // key is marked as removed
                return Optional.absent();
            }
        }
        if (value == null) {
            final Integer bucket = computeBucket(key);
            final ObjectId subtreeId = mySubTrees.get(bucket);
            if (subtreeId == null) {
                value = null;
            } else {
                RevTree subTree = db.get(subtreeId,
                        serialFactory.createRevTreeReader(db, this.depth + 1));
                return subTree.get(key);
            }
        }
        return Optional.fromNullable(value);
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

    /**
     * @return {@code true}, since this is an immutable tree. {@link MutableRevSHA1Tree} overrides
     *         this method to check against its mutable state.
     */
    @Override
    public boolean isNormalized() {
        return true;
    }

    /**
     * @return a {@code String} representation of this class
     */
    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[size: ")
                .append(this.myEntries.size()).append(", order: ").append(this.depth)
                .append(", subtrees: ").append(this.mySubTrees.size()).append(']').toString();
    }

    /**
     * Provides an iterator to iterate through the {@link NodeRef}s of the tree, skipping nodes that
     * match the provided filter.
     * 
     * @param filter the filter to use
     * @return an iterator that iterates through nodes that do not get filtered
     */
    @SuppressWarnings("unchecked")
    @Override
    public Iterator<NodeRef> iterator(Predicate<NodeRef> filter) {
        Preconditions
                .checkState(isNormalized(),
                        "iterator() should only be called on a normalized tree to account for element deletions");
        if (filter == null) {
            filter = Predicates.alwaysTrue();
        }

        if (myEntries.isEmpty() && mySubTrees.isEmpty()) {
            return Collections.EMPTY_SET.iterator();
        }
        if (!mySubTrees.isEmpty()) {
            Iterator<NodeRef>[] iterators = new Iterator[mySubTrees.size()];
            int i = 0;
            for (ObjectId subtreeId : mySubTrees.values()) {
                iterators[i] = new LazySubtreeIterator(this.db, subtreeId, this.depth + 1, filter);
                i++;
            }
            return Iterators.concat(iterators);
        }

        // we have only content entries, return them in our internal order
        Map<ObjectId, NodeRef> sorted = new TreeMap<ObjectId, NodeRef>();
        for (NodeRef ref : myEntries.values()) {
            if (filter.apply(ref)) {
                sorted.put(ObjectId.forString(ref.getPath()), ref);
            }
        }
        return sorted.values().iterator();
    }

    private class LazySubtreeIterator implements Iterator<NodeRef> {

        private final ObjectDatabase db;

        private final ObjectId objectId;

        private final int depth;

        private final Predicate<NodeRef> filter;

        private Iterator<NodeRef> subject;

        public LazySubtreeIterator(ObjectDatabase db, ObjectId objectId, int depth,
                Predicate<NodeRef> filter) {
            this.db = db;
            this.objectId = objectId;
            this.depth = depth;
            this.filter = filter;
        }

        public boolean hasNext() {
            if (subject == null) {
                RevTree subtree;
                subtree = db.get(objectId, serialFactory.createRevTreeReader(db, depth));
                subject = subtree.iterator(filter);
            }
            return subject.hasNext();
        }

        public NodeRef next() {
            return subject.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
