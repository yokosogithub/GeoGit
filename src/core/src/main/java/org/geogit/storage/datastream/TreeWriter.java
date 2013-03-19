/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.vividsolutions.jts.geom.Envelope;

public class TreeWriter implements ObjectWriter<RevTree> {
    @Override
    public void write(RevTree tree, OutputStream out) throws IOException {
        DataOutput data = new DataOutputStream(out);
        FormatCommon.writeHeader(data, "tree");
        data.writeLong(tree.size());
        data.writeInt(tree.numTrees());
        if (tree.features().isPresent()) {
            data.writeInt(tree.features().get().size());
            ImmutableList<Node> features = tree.features().get();
            for (Node feature : features) {
                writeNode(feature, data);
            }
        } else {
            data.writeInt(0);
        }
        if (tree.trees().isPresent()) {
            data.writeInt(tree.trees().get().size());
            ImmutableList<Node> subTrees = tree.trees().get();
            for (Node subTree : subTrees) {
                writeNode(subTree, data);
            }
        } else {
            data.writeInt(0);
        }
        if (tree.buckets().isPresent()) {
            data.writeInt(tree.buckets().get().size());
            ImmutableSortedMap<Integer, Bucket> buckets = tree.buckets().get();
            for (Map.Entry<Integer, Bucket> bucket : buckets.entrySet()) {
                writeBucket(bucket.getKey(), bucket.getValue(), data);
            }
        } else {
            data.writeInt(0);
        }
    }

    private void writeNode(Node node, DataOutput data) throws IOException {
        data.writeUTF(node.getName());
        data.write(node.getObjectId().getRawValue());
        data.write(node.getMetadataId().or(ObjectId.NULL).getRawValue());
        int typeN = Arrays.asList(RevObject.TYPE.values()).indexOf(node.getType());
        data.writeByte(typeN);
        Envelope envelope = new Envelope();
        node.expand(envelope);
        writeBoundingBox(envelope, data);
    }

    private void writeBucket(int index, Bucket bucket, DataOutput data) throws IOException {
        data.writeInt(index);
        data.write(bucket.id().getRawValue());
        Envelope e = new Envelope();
        bucket.expand(e);
        writeBoundingBox(e, data);
    }

    private void writeBoundingBox(Envelope bbox, DataOutput data) throws IOException {
        data.writeDouble(bbox.getMinX());
        data.writeDouble(bbox.getMaxX());
        data.writeDouble(bbox.getMinY());
        data.writeDouble(bbox.getMaxY());
    }
}
