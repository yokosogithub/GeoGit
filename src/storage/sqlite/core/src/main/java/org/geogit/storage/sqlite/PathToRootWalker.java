/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.geogit.api.ObjectId;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Walks a path from the specified node to a root, bifurcating along the way in cases where a node
 * has multiple parents.
 * 
 * @author Justin Deoliveira, Boundless
 * 
 * @param <C> Connection type.
 */
public class PathToRootWalker<T> implements Iterator<List<ObjectId>> {

    SQLiteGraphDatabase<T> graph;

    T cx;

    Queue<ObjectId> q;

    Set<ObjectId> seen;

    public PathToRootWalker(ObjectId start, SQLiteGraphDatabase<T> graph, T cx) {
        this.graph = graph;
        this.cx = cx;

        q = Lists.newLinkedList();
        q.add(start);

        seen = Sets.newHashSet();
    }

    @Override
    public boolean hasNext() {
        return !q.isEmpty();
    }

    @Override
    public List<ObjectId> next() {
        List<ObjectId> curr = Lists.newArrayList();
        List<ObjectId> next = Lists.newArrayList();

        while (!q.isEmpty()) {
            ObjectId node = q.poll();
            curr.add(node);

            Iterables.addAll(next, Iterables.transform(graph.outgoing(node.toString(), cx),
                    StringToObjectId.INSTANCE));
        }

        seen.addAll(curr);
        q.addAll(next);
        return curr;
    }

    public boolean seen(ObjectId node) {
        return seen.contains(node);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
