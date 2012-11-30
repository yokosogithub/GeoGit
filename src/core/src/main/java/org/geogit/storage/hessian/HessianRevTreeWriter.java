/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectWriter;

import com.caucho.hessian.io.Hessian2Output;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Writes a {@link RevTree tree} to a binary encoded stream.
 */

class HessianRevTreeWriter extends HessianRevWriter implements ObjectWriter<RevTree> {
    private final RevTree tree;

    /**
     * Constructs a new {@code HessianRevTreeWriter} with the given {@link RevTree}.
     * 
     * @param tree the tree to write
     */
    public HessianRevTreeWriter(RevTree tree) {
        this.tree = tree;
    }

    /**
     * Writes the provided {@link RevTree} to the output stream.
     * 
     * @param the stream to write to
     */
    @Override
    public void write(OutputStream out) throws IOException {
        RevTree revTree = this.tree;
        Hessian2Output hout = new Hessian2Output(out);
        try {
            hout.startMessage();
            hout.writeInt(BlobType.REVTREE.getValue());

            final long size = revTree.size();
            hout.writeLong(size);

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
        for (Node ref : children) {
            HessianRevTreeWriter.this.writeNode(hout, ref);
        }
    }

    private void writeBuckets(Hessian2Output hout, ImmutableSortedMap<Integer, ObjectId> buckets)
            throws IOException {

        for (Entry<Integer, ObjectId> entry : buckets.entrySet()) {
            hout.writeInt(HessianRevReader.Node.BUCKET.getValue());
            hout.writeInt(entry.getKey().intValue());
            HessianRevTreeWriter.this.writeObjectId(hout, entry.getValue());
        }
    }
}
