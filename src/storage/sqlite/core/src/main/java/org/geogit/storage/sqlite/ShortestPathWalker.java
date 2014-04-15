/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.sqlite;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import org.geogit.api.ObjectId;

import com.google.common.collect.Maps;

/**
 * Walks a shortest path between two nodes applying Dijkstra's algorithm.
 * 
 * @author Justin Deoliveira, Boundless
 * 
 * @param <C> Connection type.
 */
public class ShortestPathWalker<T> implements Iterator<ObjectId> {

    final ObjectId start;

    final ObjectId end;

    final SQLiteGraphDatabase<T> graph;

    final T cx;

    final Map<String, Node> nodes;

    final PriorityQueue<Node> q;

    ShortestPathWalker(ObjectId start, ObjectId end, SQLiteGraphDatabase<T> graph, T cx) {
        this.start = start;
        this.end = end;
        this.graph = graph;
        this.cx = cx;

        nodes = Maps.newHashMap();
        q = new PriorityQueue<Node>(100, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return o1.cost.compareTo(o2.cost);
            }
        });
        q.offer(newNode(start.toString(), 0d));
    }

    Node newNode(String id, Double cost) {
        Node n = new Node(id, cost);
        nodes.put(id, n);
        return n;
    }

    @Override
    public boolean hasNext() {
        return !q.isEmpty();
    }

    @Override
    public ObjectId next() {
        // grab next node
        Node n = q.poll();

        // update the adjacent nodes
        for (String adj : graph.outgoing(n.id, cx)) {
            Node m = nodes.get(adj);
            Double cost = n.cost + 1;

            if (m == null) {
                m = newNode(adj, cost);
                q.offer(m);
            } else {
                if (cost < m.cost) {
                    // update the node
                    m.cost = cost;
                    q.remove(m);
                    q.offer(m);
                }
            }
        }

        return ObjectId.valueOf(n.id);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    static class Node {
        String id;

        Double cost = Double.MAX_VALUE;

        Node(String id, Double cost) {
            this.id = id;
            this.cost = cost;
        }
    }
}
