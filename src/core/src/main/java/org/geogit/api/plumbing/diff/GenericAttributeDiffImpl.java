/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import org.geogit.storage.text.TextValueSerializer;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Generic implementation of a difference between two values for a given attribute
 * 
 */
public class GenericAttributeDiffImpl implements AttributeDiff {

    /**
     * The new value. Null if it does not exist (the attribute has been removed)
     */
    private Optional<?> newValue;

    /**
     * The old value. Null if it did not exist (the attribute has been added)
     */
    private Optional<?> oldValue;

    public GenericAttributeDiffImpl(Optional<?> oldValue, Optional<?> newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public TYPE getType() {
        TYPE type = (newValue == null || !newValue.isPresent()) ? TYPE.REMOVED
                : (oldValue == null || !oldValue.isPresent()) ? TYPE.ADDED : TYPE.MODIFIED;
        return type;
    }

    @Override
    public Optional<?> getOldValue() {
        return oldValue;
    }

    @Override
    public Optional<?> getNewValue() {
        return newValue;
    }

    public String toString() {
        if (getType().equals(TYPE.MODIFIED)) {
            return attributeValueAsString(oldValue) + " -> " + attributeValueAsString(newValue);
        } else if (getType().equals(TYPE.ADDED)) {
            return "[MISSING] -> " + attributeValueAsString(newValue);
        } else {
            return attributeValueAsString(oldValue) + " -> [MISSING]";
        }

    }

    private CharSequence attributeValueAsString(Optional<?> value) {
        return TextValueSerializer.asString(Optional.fromNullable((Object) value.orNull()));
    }

    @Override
    public AttributeDiff reversed() {
        return new GenericAttributeDiffImpl(newValue, oldValue);
    }

    @Override
    public Optional<?> applyOn(Optional<?> obj) {
        Preconditions.checkState(canBeAppliedOn(obj));
        return newValue;
    }

    @Override
    public boolean canBeAppliedOn(Optional<?> obj) {
        if (obj == null) {
            return oldValue == null;
        }
        return obj.equals(oldValue);
    }

    @Override
    public String asText() {
        if (getType().equals(TYPE.MODIFIED)) {
            return getType().name().toCharArray()[0] + "\t" + attributeValueAsString(oldValue)
                    + "\t" + attributeValueAsString(newValue);
        } else if (getType().equals(TYPE.ADDED)) {
            return getType().name().toCharArray()[0] + "\t" + attributeValueAsString(newValue);
        } else {
            return getType().name().toCharArray()[0] + "\t" + attributeValueAsString(oldValue);
        }
    }

    @Override
    public boolean equals(Object o) {
        // TODO: this is a temporary simple comparison. Should be more elaborate
        if (!(o instanceof GenericAttributeDiffImpl)) {
            return false;
        }
        GenericAttributeDiffImpl d = (GenericAttributeDiffImpl) o;
        return d.oldValue.equals(oldValue) && d.newValue.equals(newValue);
    }

    @Override
    public boolean conflicts(AttributeDiff ad) {
        if (ad instanceof GenericAttributeDiffImpl) {
            GenericAttributeDiffImpl gad = (GenericAttributeDiffImpl) ad;
            return !Objects.equal(gad.newValue, newValue);
        } else {
            return true;
        }
    }

}
