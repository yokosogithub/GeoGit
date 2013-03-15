package org.geogit.api.plumbing.diff;

import org.geogit.storage.text.AttributeValueSerializer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class GenericAttributeDiffImpl implements AttributeDiff {

    private Optional<?> newValue;

    private Optional<?> oldValue;

    public GenericAttributeDiffImpl(Optional<?> oldValue, Optional<?> newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public TYPE getType() {
        TYPE type = newValue == null ? TYPE.REMOVED : oldValue == null ? TYPE.ADDED : TYPE.MODIFIED;
        return type;
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

    private CharSequence attributeValueAsString(Optional<?> opt) {
        Object value = opt.orNull();
        return AttributeValueSerializer.asText(value);
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
            return !gad.newValue.equals(newValue);
        } else {
            return true;
        }
    }

}
