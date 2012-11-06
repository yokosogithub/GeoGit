/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.storage.RefDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Update the object name stored in a {@link Ref} safely.
 * <p>
 * 
 */
public class UpdateRef extends AbstractGeoGitOp<Optional<Ref>> {

    private String name;

    private ObjectId newValue;

    private ObjectId oldValue;

    private boolean delete;

    private String reason;

    private RefDatabase refDb;

    /**
     * Constructs a new {@code UpdateRef} operation with the given {@link RefDatabase reference
     * database}.
     * 
     * @param refDb the reference database to use.
     */
    @Inject
    public UpdateRef(RefDatabase refDb) {
        this.refDb = refDb;
    }

    /**
     * @param name the name of the ref to update
     * @return this
     */
    public UpdateRef setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param newValue the value to set the reference to. It can be an object id
     *        {@link ObjectId#toString() hash code} or a symbolic name such as
     *        {@code "refs/origin/master"}
     * @return this
     */
    public UpdateRef setNewValue(ObjectId newValue) {
        this.newValue = newValue;
        return this;
    }

    /**
     * @param oldValue if provided, the operation will fail if the current ref value doesn't match
     *        {@code oldValue}
     * @return this
     */
    public UpdateRef setOldValue(ObjectId oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    /**
     * @param delete if {@code true}, the ref will be deleted
     * @return this
     */
    public UpdateRef setDelete(boolean delete) {
        this.delete = delete;
        return this;
    }

    /**
     * @param reason if provided, the ref log will be updated with this reason message
     * @return this
     * @TODO: reflog not yet implemented
     */
    public UpdateRef setReason(String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * Executes the operation.
     * 
     * @return the new value of the ref
     */
    @Override
    public Optional<Ref> call() {
        Preconditions.checkState(name != null, "name has not been set");
        Preconditions.checkState(delete || newValue != null, "value has not been set");

        String storedValue = refDb.getRef(name);
        Preconditions.checkState(oldValue == null || oldValue.toString().equals(storedValue),
                "Old value (" + storedValue + ") doesn't match expected value '" + oldValue + "'");

        if (delete) {
            refDb.remove(name);
        } else {
            refDb.putRef(name, newValue.toString());
        }
        return command(RefParse.class).setName(name).call();
    }

}
