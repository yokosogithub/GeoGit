/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeImpl;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianProtocolException;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Maps;

/**
 * Reads {@link RevTree trees} from a binary encoded stream.
 * 
 */
class HessianRevTreeReader extends HessianRevReader implements ObjectReader<RevTree> {

    public HessianRevTreeReader() {
    }

    @Override
    public RevTree read(ObjectId id, InputStream rawData) throws IllegalArgumentException {
        Hessian2Input hin = new Hessian2Input(rawData);
        try {
            hin.startMessage();
            BlobType blobType = BlobType.fromValue(hin.readInt());
            if (blobType != BlobType.REVTREE)
                throw new IllegalArgumentException("Could not parse blob of type " + blobType
                        + " as rev tree.");

            // BigInteger size = new BigInteger(hin.readBytes());
            // BigInteger size = BigInteger.ZERO;

            Builder<NodeRef> children = ImmutableList.builder();
            TreeMap<Integer, ObjectId> subtrees = Maps.newTreeMap();

            while (true) {
                Node type = null;
                try {
                    type = Node.fromValue(hin.readInt());
                } catch (HessianProtocolException ex) {
                    ex.printStackTrace();
                }
                if (type.equals(Node.REF)) {
                    NodeRef entryRef = readNodeRef(hin);
                    children.add(entryRef);
                } else if (type.equals(Node.TREE)) {
                    parseAndSetSubTree(hin, subtrees);
                } else if (type.equals(Node.END)) {
                    break;
                }
            }

            hin.completeMessage();

            RevTree tree;
            if (subtrees.isEmpty()) {
                tree = RevTreeImpl.createLeafTree(id, children.build());
            } else {
                tree = RevTreeImpl.createNodeTree(id, subtrees);
            }
            return tree;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void parseAndSetSubTree(Hessian2Input hin, TreeMap<Integer, ObjectId> subtrees)
            throws IOException {
        int bucket = hin.readInt();
        ObjectId id = readObjectId(hin);
        subtrees.put(Integer.valueOf(bucket), id);
    }
}
