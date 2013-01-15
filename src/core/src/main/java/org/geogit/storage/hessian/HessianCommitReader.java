/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Preconditions;

/**
 * Reads {@link RevCommit commits} from a binary encoded stream.
 */
class HessianCommitReader extends HessianRevReader<RevCommit> implements ObjectReader<RevCommit> {

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
    protected RevCommit read(ObjectId id, Hessian2Input hin, RevObject.TYPE type)
            throws IOException {
        Preconditions.checkArgument(RevObject.TYPE.COMMIT.equals(type));
        CommitBuilder builder = new CommitBuilder();

        builder.setTreeId(readObjectId(hin));
        int parentCount = hin.readInt();
        List<ObjectId> pIds = new ArrayList<ObjectId>(parentCount);
        for (int i = 0; i < parentCount; i++) {
            pIds.add(readObjectId(hin));
        }
        builder.setParentIds(pIds);
        builder.setAuthor(hin.readString());
        builder.setAuthorEmail(hin.readString());
        builder.setAuthorTimestamp(hin.readLong());
        builder.setAuthorTimeZoneOffset(hin.readInt());
        builder.setCommitter(hin.readString());
        builder.setCommitterEmail(hin.readString());
        builder.setCommitterTimestamp(hin.readLong());
        builder.setCommitterTimeZoneOffset(hin.readInt());
        builder.setMessage(hin.readString());

        RevCommit commit = builder.build();
        Preconditions.checkState(id.equals(commit.getId()));

        return commit;
    }
}
