/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;

import com.google.common.base.Throwables;

@Deprecated
public class BlobReader implements ObjectReader<RevFeature> {

    @Override
    public RevFeature read(ObjectId id, InputStream rawData) throws IllegalArgumentException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int c;
        try {
            while ((c = rawData.read()) != -1) {
                output.write(c);
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return new RevFeature(id, output.toByteArray());
    }

}
