/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage;

import java.io.Serializable;

import org.geogit.api.Node;

import com.google.common.collect.Ordering;

/**
 * Implements storage order of {@link Node} based on its name using a {@link NodePathStorageOrder}
 * comparator.
 * 
 * @see NodePathStorageOrder
 */
public final class NodeStorageOrder extends Ordering<Node> implements Serializable {

    private static final long serialVersionUID = -2860468212633430368L;

    private final NodePathStorageOrder nameOrder = new NodePathStorageOrder();

    @Override
    public int compare(Node nr1, Node nr2) {
        return nameOrder.compare(nr1.getName(), nr2.getName());
    }

    /**
     * @see NodePathStorageOrder#bucket(String, int)
     */
    public Integer bucket(final Node ref, final int depth) {
        return nameOrder.bucket(ref.getName(), depth);
    }
}