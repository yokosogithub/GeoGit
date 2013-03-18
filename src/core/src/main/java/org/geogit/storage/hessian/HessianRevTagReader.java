/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTag;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Preconditions;

/**
 * Reads {@link RevTag tags} from a binary encoded stream.
 * 
 */
class HessianRevTagReader extends HessianRevReader<RevTag> implements ObjectReader<RevTag> {

    public HessianRevTagReader() {
    }

    @Override
    protected RevTag read(ObjectId id, Hessian2Input hin, RevObject.TYPE blobType)
            throws IOException {
        Preconditions.checkArgument(RevObject.TYPE.TAG.equals(blobType));

        String name = hin.readString();
        String message = hin.readString();
        ObjectId commitId = readObjectId(hin);
        String taggerName = hin.readString();
        String taggerEmail = hin.readString();
        long taggerTimestamp = hin.readLong();
        int taggerTimeZoneOffset = hin.readInt();
        RevPerson tagger = new RevPerson(taggerName, taggerEmail, taggerTimestamp,
                taggerTimeZoneOffset);

        return new RevTag(id, name, commitId, message, tagger);
    }

}
