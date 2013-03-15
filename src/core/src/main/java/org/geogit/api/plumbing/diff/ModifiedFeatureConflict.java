package org.geogit.api.plumbing.diff;

import java.util.List;

import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.collect.Lists;

public class ModifiedFeatureConflict {

    private List<ConflictedAttribute> conflicted;

    public ModifiedFeatureConflict() {
        conflicted = Lists.newArrayList();
    }

    public void addConflictedAttribute(PropertyDescriptor pd, AttributeDiff ad, AttributeDiff ad2) {
        conflicted.add(new ConflictedAttribute(pd, ad, ad2));

    }

    public boolean isEmpty() {
        return conflicted.isEmpty();
    }

    public class ConflictedAttribute {

        private PropertyDescriptor pd;

        private AttributeDiff ad;

        private Object ad2;

        ConflictedAttribute(PropertyDescriptor pd, AttributeDiff ad, AttributeDiff ad2) {
            this.pd = pd;
            this.ad = ad;
            this.ad2 = ad2;
        }

        public PropertyDescriptor getPropertyDescriptor() {
            return pd;
        }

        public AttributeDiff getDifference() {
            return ad;
        }

        public Object getAlternativeDifference() {
            return ad2;
        }

    }

}
