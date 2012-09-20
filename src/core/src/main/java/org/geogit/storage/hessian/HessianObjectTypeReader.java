/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;
import java.io.InputStream;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Throwables;

class HessianObjectTypeReader extends HessianRevReader implements ObjectReader<RevObject.TYPE> {

    @Override
    public RevObject.TYPE read(ObjectId id, InputStream rawData) {
        Hessian2Input hin = new Hessian2Input(rawData);

        try {
            hin.startMessage();
            BlobType type = BlobType.fromValue(hin.readInt());
            switch (type) {
            case COMMIT:
                return RevObject.TYPE.COMMIT;
            case FEATURE:
                return RevObject.TYPE.FEATURE;
            case REVTREE:
                return RevObject.TYPE.TREE;
            case FEATURETYPE:
                return RevObject.TYPE.FEATURETYPE;
            default:
                throw new IllegalArgumentException("Unknown object type " + type);
            }
            // hin.completeMessage();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                hin.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
