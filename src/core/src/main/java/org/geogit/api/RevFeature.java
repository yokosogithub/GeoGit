/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A binary representation of the state of a Feature.
 * 
 */
public class RevFeature extends AbstractRevObject {

    private final ImmutableList<Optional<Object>> values;

    public RevFeature(ImmutableList<Optional<Object>> values) {
        this(ObjectId.NULL, values);
    }

    public RevFeature(ObjectId id, ImmutableList<Optional<Object>> values) {
        super(id, TYPE.FEATURE);
        this.values = values;
    }

    public ImmutableList<Optional<Object>> getValues() {
        return values;
    }
}
