/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.OutputStream;

import org.geogit.api.RevObject;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTag;
import org.geogit.storage.ObjectWriter;

import com.caucho.hessian.io.Hessian2Output;

/**
 * Writes a {@link RevTag tag} to a binary encoded stream.
 */

class HessianRevTagWriter extends HessianRevWriter implements ObjectWriter<RevTag> {

    /**
     * Writes the provided {@link RevTag} to the output stream.
     * 
     * @param the stream to write to
     */
    @Override
    public void write(final RevTag revTag, OutputStream out) throws IOException {
        Hessian2Output hout = new Hessian2Output(out);
        try {
            hout.startMessage();
            hout.writeInt(RevObject.TYPE.TAG.value());
            hout.writeString(revTag.getName());
            hout.writeString(revTag.getMessage());
            writeObjectId(hout, revTag.getCommitId());
            writePerson(hout, revTag.getTagger());
            hout.completeMessage();
        } finally {
            hout.flush();
        }
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
