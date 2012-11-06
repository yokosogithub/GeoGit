/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Throwables;

/**
 * Reads {@link RevCommit commits} from a binary encoded stream.
 */
class HessianCommitReader extends HessianRevReader implements ObjectReader<RevCommit> {

    /**
     * Reads a {@link RevCommit} from the given input stream and assigns it the provided
     * {@link ObjectId id}.
     * 
     * @param id the id to use for the commit
     * @param rawData the input stream of the commit
     * @return the final commit
     * @throws IllegalArgumentException if the provided stream does not represent a
     *         {@code RevCommit}
     */
    @Override
    public RevCommit read(ObjectId id, InputStream rawData) throws IllegalArgumentException {
        Hessian2Input hin = new Hessian2Input(rawData);
        CommitBuilder builder = new CommitBuilder();

        try {
            hin.startMessage();
            BlobType type = BlobType.fromValue(hin.readInt());
            if (type != BlobType.COMMIT)
                throw new IllegalArgumentException("Could not parse blob of type " + type
                        + " as a commit.");

            builder.setTreeId(readObjectId(hin));
            int parentCount = hin.readInt();
            List<ObjectId> pIds = new ArrayList<ObjectId>(parentCount);
            for (int i = 0; i < parentCount; i++) {
                pIds.add(readObjectId(hin));
            }
            builder.setParentIds(pIds);
            builder.setAuthor(hin.readString());
            builder.setAuthorEmail(hin.readString());
            builder.setCommitter(hin.readString());
            builder.setCommitterEmail(hin.readString());
            builder.setMessage(hin.readString());
            builder.setTimestamp(hin.readLong());

            hin.completeMessage();
            /*
             * @TODO: revisit. It looks like hessian doesn't produce consistent blobs. If we used
             * the two commented out lines bellow instead, IndexTest.testWriteTree2 fails
             * unpredictable at the check for id equality. In principle, it would be a good thing
             * for us to check that the read object's generated id (through HashObject) corresponds
             * to the id the object is being retrieved with.
             */
            // RevCommit commit = builder.build();
            // checkState(id.equals(commit.getId()));

            RevCommit commit = builder.build(id);
            return commit;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
