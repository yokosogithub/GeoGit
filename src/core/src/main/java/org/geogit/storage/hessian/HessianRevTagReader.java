/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.IOException;

import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.RevTree;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Preconditions;

/**
 * Reads {@link RevTree trees} from a binary encoded stream.
 * 
 */
class HessianRevTagReader extends HessianRevReader<RevTree> implements ObjectReader<RevTree> {

    public HessianRevTagReader() {
    }

    @Override
    protected RevTree read(ObjectId id, Hessian2Input hin, RevObject.TYPE blobType)
            throws IOException {
        Preconditions.checkArgument(RevObject.TYPE.TAG.equals(blobType));
        throw new UnsupportedOperationException("tags not yet implemented");
    }

}
