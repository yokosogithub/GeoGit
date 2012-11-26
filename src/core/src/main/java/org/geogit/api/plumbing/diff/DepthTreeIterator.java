/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;

import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Iterators;

/**
 * An iterator over a {@link RevTree} that fully traverses it, including any sub tree if
 * {@link #setTraverseSubtrees(boolean) traverseSubtrees == true}.
 */
public class DepthTreeIterator extends AbstractIterator<NodeRef> {

    private RevTree tree;

    private Iterator<NodeRef> iterator;

    private ObjectDatabase source;

    private ObjectSerialisingFactory serialFactory;

    private boolean traverseSubtrees;

    private boolean onlyTrees;

    private boolean includeTrees;

    public DepthTreeIterator(RevTree tree, ObjectDatabase source,
            ObjectSerialisingFactory serialFactory) {

        checkNotNull(tree);
        checkNotNull(source);
        checkNotNull(serialFactory);

        this.tree = tree;
        this.source = source;
        this.serialFactory = serialFactory;
    }

    /**
     * @param traverseSubtrees if {@code true}, any tree {@link NodeRef} will be resolved to its
     *        corresponding {@link RevTree} and its children feature {@link NodeRef}s will be
     *        returned. Defaults to {@code false}
     */
    public void setTraverseSubtrees(boolean traverseSubtrees) {
        this.traverseSubtrees = traverseSubtrees;
    }

    public boolean isTraverseSubtrees() {
        return traverseSubtrees;
    }

    @Override
    protected NodeRef computeNext() {
        if (iterator == null) {
            iterator = resolveIterator();
        }
        if (iterator.hasNext()) {
            NodeRef ref = iterator.next();
            if (ref.getType().equals(TYPE.TREE)) {
                if (traverseSubtrees) {
                    ObjectId subtreeId = ref.getObjectId();
                    RevTree subTree = source.get(subtreeId, serialFactory.createRevTreeReader());
                    DepthTreeIterator subtreeIterator = new DepthTreeIterator(subTree, source,
                            serialFactory);
                    subtreeIterator.setTraverseSubtrees(true);
                    subtreeIterator.setIncludeTrees(includeTrees);
                    subtreeIterator.setOnlyTrees(onlyTrees);
                    this.iterator = Iterators.concat(subtreeIterator, this.iterator);
                }
                if (!includeTrees && !onlyTrees) {
                    return computeNext();
                } else {
                    return ref;
                }
            } else {
                if (onlyTrees) {
                    return computeNext();
                } else {
                    return ref;
                }
            }

        }
        return endOfData();
    }

    private Iterator<NodeRef> resolveIterator() {
        if (tree.isEmpty()) {
            return Iterators.emptyIterator();
        } else if (tree.children().isPresent()) {
            return tree.children().get().iterator();
        }

        ImmutableCollection<ObjectId> bucketTreeIds = tree.buckets().get().values();
        return new BucketIterator(bucketTreeIds, source, serialFactory);
    }

    /**
     * An iterator over a {@link RevTree}'s buckets
     */
    class BucketIterator extends AbstractIterator<NodeRef> {

        private final ImmutableCollection<ObjectId> bucketTreeIds;

        private final ObjectDatabase source;

        private final ObjectSerialisingFactory serialFactory;

        private Iterator<NodeRef> iterator;

        public BucketIterator(final ImmutableCollection<ObjectId> bucketTreeIds,
                ObjectDatabase source, ObjectSerialisingFactory serialFactory) {

            this.bucketTreeIds = bucketTreeIds;
            this.source = source;
            this.serialFactory = serialFactory;
        }

        @Override
        protected NodeRef computeNext() {
            if (iterator == null) {
                iterator = resolveIterator();
            }

            if (iterator.hasNext()) {
                return iterator.next();
            }
            return endOfData();
        }

        /**
         * @return
         */
        private Iterator<NodeRef> resolveIterator() {
            Function<ObjectId, Iterator<NodeRef>> bucketToTreeIterator = new Function<ObjectId, Iterator<NodeRef>>() {

                public Iterator<NodeRef> apply(ObjectId treeId) {

                    RevTree tree = source.get(treeId, serialFactory.createRevTreeReader());
                    DepthTreeIterator treeIterator;
                    treeIterator = new DepthTreeIterator(tree, BucketIterator.this.source,
                            BucketIterator.this.serialFactory);
                    treeIterator.setTraverseSubtrees(DepthTreeIterator.this.traverseSubtrees);
                    return treeIterator;
                }
            };

            Collection<Iterator<NodeRef>> bucketIterators = Collections2.transform(bucketTreeIds,
                    bucketToTreeIterator);
            return Iterators.concat(bucketIterators.iterator());
        }

    }

    /**
     * 
     * @param includeTrees if {@code true}, only {@link NodeRef} resolving to a tree are returned
     */
    public void setOnlyTrees(boolean onlyTrees) {
        this.onlyTrees = onlyTrees;
    }

    /**
     * 
     * @param includeTrees if {@code true}, any {@link NodeRef} resolving to a tree will be returned
     *        along with its children. If not, only children are returned.
     */
    public void setIncludeTrees(boolean includeTrees) {
        this.includeTrees = includeTrees;
    }

}
