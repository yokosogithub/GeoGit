/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A binary representation of the values of a Feature.
 * 
 */
public class RevFeature extends AbstractRevObject {

    private final ImmutableList<Optional<Object>> values;

    /**
     * Constructs a new {@code RevFeature} with the provided set of values.
     * 
     * @param values a list of values, with {@link Optional#absent()} representing a null value
     */
    public RevFeature(ImmutableList<Optional<Object>> values) {
        this(ObjectId.NULL, values);
    }

    /**
     * Constructs a new {@code RevFeature} with the provided {@link ObjectId} and set of values
     * 
     * @param id the {@link ObjectId} to use for this feature
     * @param values a list of values, with {@link Optional#absent()} representing a null value
     */
    public RevFeature(ObjectId id, ImmutableList<Optional<Object>> values) {
        super(id);
        this.values = values;
    }

    /**
     * @return a list of values, with {@link Optional#absent()} representing a null value
     */
    public ImmutableList<Optional<Object>> getValues() {
        return values;
    }

    @Override
    public TYPE getType() {
        return TYPE.FEATURE;
    }
}
