/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage;

import org.geogit.api.NodeRef;

import com.google.common.collect.Ordering;

/**
 * Implements storage order of {@link NodeRef} based on its {@link #pathHash(NodeRef) hashed path}
 */
public final class NodeRefStorageOrder extends Ordering<NodeRef> {

    private final NodeRefPathStorageOrder pathOrder = new NodeRefPathStorageOrder();

    @Override
    public int compare(NodeRef nr1, NodeRef nr2) {
        return pathOrder.compare(nr1.getPath(), nr2.getPath());
    }
}