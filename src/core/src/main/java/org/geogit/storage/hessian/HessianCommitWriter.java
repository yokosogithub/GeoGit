/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevPerson;
import org.geogit.storage.ObjectWriter;

import com.caucho.hessian.io.Hessian2Output;

/**
 * Writes {@link RevCommit commits} to a binary encoded stream.
 */
class HessianCommitWriter extends HessianRevWriter implements ObjectWriter<RevCommit> {

    private RevCommit commit;

    /**
     * Constructs a new {@code HessianCommitWriter} with the given {@link RevCommit}.
     * 
     * @param commit the commit to write
     */
    public HessianCommitWriter(final RevCommit commit) {
        this.commit = commit;
    }

    /**
     * Writes the commit to the provided output stream.
     * 
     * @param out the stream to write to
     */
    @Override
    public void write(OutputStream out) throws IOException {
        Hessian2Output hout = new Hessian2Output(out);

        hout.startMessage();
        hout.writeInt(BlobType.COMMIT.getValue());

        writeObjectId(hout, commit.getTreeId());

        List<ObjectId> parentIds = commit.getParentIds();
        hout.writeInt(parentIds.size());
        for (ObjectId pId : parentIds) {
            writeObjectId(hout, pId);
        }

        writePerson(hout, commit.getAuthor());
        writePerson(hout, commit.getCommitter());
        hout.writeString(commit.getMessage());
        long timestamp = commit.getTimestamp();
        if (timestamp <= 0) {
            timestamp = System.currentTimeMillis();
        }
        hout.writeLong(timestamp);

        hout.completeMessage();

        hout.flush();
    }

    private void writePerson(Hessian2Output hout, RevPerson person) throws IOException {
        if (person != null) {
            hout.writeString(person.getName());
            hout.writeString(person.getEmail());
        } else {
            hout.writeNull();
            hout.writeNull();
        }
    }

    @Override
    public RevCommit object() {
        return commit;
    }

}
