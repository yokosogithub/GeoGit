/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;

import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectWriter;

import com.caucho.hessian.io.Hessian2Output;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSortedMap;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Writes a {@link RevTree tree} to a binary encoded stream.
 */

class HessianRevTreeWriter extends HessianRevWriter implements ObjectWriter<RevTree> {

    /**
     * Writes the provided {@link RevTree} to the output stream.
     * 
     * @param the stream to write to
     */
    @Override
    public void write(final RevTree revTree, OutputStream out) throws IOException {
        Hessian2Output hout = new Hessian2Output(out);
        try {
            hout.startMessage();
            hout.writeInt(RevObject.TYPE.TREE.value());

            final long size = revTree.size();
            final int trees = revTree.numTrees();
            hout.writeLong(size);
            hout.writeInt(trees);

            if (revTree.trees().isPresent()) {
                writeChildren(hout, revTree.trees().get());
            }
            if (revTree.features().isPresent()) {
                writeChildren(hout, revTree.features().get());
            } else if (revTree.buckets().isPresent()) {
                writeBuckets(hout, revTree.buckets().get());
            }

            hout.writeInt(HessianRevTreeReader.Node.END.getValue());

            hout.completeMessage();
        } finally {
            hout.flush();
        }
    }

    private void writeChildren(Hessian2Output hout, ImmutableCollection<Node> children)
            throws IOException {
        Envelope envHelper = new Envelope();
        for (Node ref : children) {
            HessianRevTreeWriter.this.writeNode(hout, ref, envHelper);
        }
    }

    private void writeBuckets(Hessian2Output hout,
            ImmutableSortedMap<Integer, Bucket> immutableSortedMap) throws IOException {

        Envelope env = new Envelope();
        for (Entry<Integer, Bucket> entry : immutableSortedMap.entrySet()) {
            hout.writeInt(HessianRevReader.Node.BUCKET.getValue());
            Integer bucketIndex = entry.getKey();
            Bucket bucket = entry.getValue();
            hout.writeInt(bucketIndex.intValue());
            HessianRevTreeWriter.this.writeObjectId(hout, bucket.id());
            env.setToNull();
            bucket.expand(env);
            hout.writeDouble(env.getMinX());
            hout.writeDouble(env.getMinY());
            hout.writeDouble(env.getMaxX());
            hout.writeDouble(env.getMaxY());
        }
    }
}
