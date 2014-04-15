/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;

import org.geogit.api.ObjectId;

import com.google.common.base.Preconditions;
import com.google.common.base.Objects;

/**
 * A class to store a merge conflict. It stores the information needed to solve the conflict, saving
 * the object id's that point to the common ancestor and both versions of a given geogit element
 * that are to be merged.
 * 
 * A null ObjectId indicates that, for the corresponding version, the element did not exist
 * 
 */
public final class Conflict {

    private ObjectId ancestor;

    private ObjectId theirs;

    private ObjectId ours;

    private String path;

    public Conflict(String path, ObjectId ancestor, ObjectId ours, ObjectId theirs) {
        this.path = path;
        this.ancestor = ancestor;
        this.ours = ours;
        this.theirs = theirs;
    }

    public ObjectId getAncestor() {
        return ancestor;
    }

    public ObjectId getOurs() {
        return ours;
    }

    public ObjectId getTheirs() {
        return theirs;
    }

    public String getPath() {
        return path;
    }

    public boolean equals(Object x) {
        if (x instanceof Conflict) {
            Conflict that = (Conflict) x;
            return Objects.equal(this.ancestor, that.ancestor) &&
                   Objects.equal(this.theirs, that.theirs) &&
                   Objects.equal(this.ours, that.ours) &&
                   Objects.equal(this.path, that.path);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hashCode(ancestor, theirs, ours, path);
    }

    public String toString() {
        return path + "\t" + ancestor.toString() + "\t" + ours.toString() + "\t"
                + theirs.toString();
    }

    public static Conflict valueOf(String s) {
        String[] tokens = s.split("\t");
        Preconditions.checkArgument(tokens.length == 4, "wrong conflict definitions: %s", s);
        String path = tokens[0];
        ObjectId ancestor = ObjectId.valueOf(tokens[1]);
        ObjectId ours = ObjectId.valueOf(tokens[2]);
        ObjectId theirs = ObjectId.valueOf(tokens[3]);
        return new Conflict(path, ancestor, ours, theirs);
    }

}
