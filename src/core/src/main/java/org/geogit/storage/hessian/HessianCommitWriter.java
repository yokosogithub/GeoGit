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
import org.geogit.api.RevObject;
import org.geogit.api.RevPerson;
import org.geogit.storage.ObjectWriter;

import com.caucho.hessian.io.Hessian2Output;
import com.google.common.collect.ImmutableList;

/**
 * Writes {@link RevCommit commits} to a binary encoded stream.
 */
class HessianCommitWriter extends HessianRevWriter implements ObjectWriter<RevCommit> {

    /**
     * Writes the commit to the provided output stream.
     */
    @Override
    public void write(RevCommit commit, OutputStream out) throws IOException {
        Hessian2Output hout = new Hessian2Output(out);

        hout.startMessage();
        hout.writeInt(RevObject.TYPE.COMMIT.value());

        writeObjectId(hout, commit.getTreeId());

        ImmutableList<ObjectId> parentIds = commit.getParentIds();
        hout.writeInt(parentIds.size());
        for (ObjectId pId : parentIds) {
            writeObjectId(hout, pId);
        }

        writePerson(hout, commit.getAuthor());
        writePerson(hout, commit.getCommitter());
        hout.writeString(commit.getMessage());

        hout.completeMessage();

        hout.flush();
    }

    private void writePerson(Hessian2Output hout, RevPerson person) throws IOException {
        if (person != null) {
            if (person.getName().isPresent()) {
                hout.writeString(person.getName().get());
            } else {
                hout.writeNull();
            }

            if (person.getEmail().isPresent()) {
                hout.writeString(person.getEmail().get());
            } else {
                hout.writeNull();
            }

            hout.writeLong(person.getTimestamp());
            hout.writeInt(person.getTimeZoneOffset());

        } else {
            hout.writeNull();
            hout.writeNull();
            hout.writeLong(0L);
            hout.writeInt(0);
        }
    }

}
