/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogit.api.RevCommit;
import org.geogit.api.RevFeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * A container for blame information. It just stores the commit of the last modification for each
 * attribute of a given feature
 * 
 */
public class BlameReport {

    List<String> attributes;

    private HashMap<String, RevCommit> changes;

    public BlameReport(RevFeatureType featureType) {
        attributes = Lists.newArrayList();
        for (PropertyDescriptor attribute : featureType.sortedDescriptors()) {
            attributes.add(attribute.getName().getLocalPart().toString());
        }
        this.changes = new HashMap<String, RevCommit>();

    }

    /**
     * Returns true if there is a commit associated to each attribute in this report
     * 
     * @return
     */
    public boolean isComplete() {
        return this.changes.size() == attributes.size();
    }

    /**
     * Reports an attribute as changed by a commit. If that attribute is not present in the report,
     * it will be added, and marked as added by the passed commit. If it is already added, calling
     * this method has no effect.
     * 
     * @param attribute the attribute changed
     * @param commit the commit that changed the passed attribute
     */
    public void addDiff(String attribute, RevCommit commit) {
        if (attributes.contains(attribute)) {
            if (!changes.containsKey(attribute)) {
                changes.put(attribute, commit);
            }
        }
    }

    /**
     * Returns the map of changes
     * 
     */
    public Map<String, RevCommit> getChanges() {
        return ImmutableMap.copyOf(changes);
    }

    /**
     * Sets all the missing attributes as having been modified for the last time by the passed
     * commit.
     * 
     * @param commit
     */
    public void setFirstCommit(RevCommit commit) {
        for (String attr : attributes) {
            if (!changes.containsKey(attr)) {
                changes.put(attr, commit);
            }
        }

    }

}
