/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.storage.RefDatabase;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Update the object name stored in a {@link Ref} safely.
 * <p>
 * 
 */
public class UpdateSymRef extends AbstractGeoGitOp<Optional<SymRef>> {

    private String name;

    private String newValue;

    private String oldValue;

    private boolean delete;

    private String reason;

    private RefDatabase refDb;

    @Inject
    public UpdateSymRef(RefDatabase refDb) {
        this.refDb = refDb;
    }

    /**
     * @param name the name of the ref to update
     */
    public UpdateSymRef setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param newValue the value to set the reference to. It can be an object id
     *        {@link ObjectId#toString() hash code} or a symbolic name such as
     *        {@code "refs/origin/master"}
     */
    public UpdateSymRef setNewValue(String newValue) {
        this.newValue = newValue;
        return this;
    }

    /**
     * @param oldValue if provided, the operation will fail if the current ref value doesn't match
     *        {@code oldValue}
     */
    public UpdateSymRef setOldValue(String oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    /**
     * @param delete if {@code true}, the ref will be deleted
     */
    public UpdateSymRef setDelete(boolean delete) {
        this.delete = delete;
        return this;
    }

    /**
     * @param reason if provided, the ref log will be updated with this reason message
     * @TODO: reflog not yet implemented
     */
    public UpdateSymRef setReason(String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * The new ref
     */
    @Override
    public Optional<SymRef> call() {
        Preconditions.checkState(name != null, "name has not been set");
        Preconditions.checkState(delete || newValue != null, "value has not been set");

        String storedValue = refDb.getSymRef(name);
        Preconditions.checkState(oldValue == null || oldValue.equals(storedValue), "Old value ("
                + storedValue + ") doesn't match expected value '" + oldValue + "'");

        if (delete) {
            refDb.remove(name);
        } else {
            refDb.putSymRef(name, newValue);
        }
        Optional<Ref> ref = command(RefParse.class).setName(name).call();
        if (!ref.isPresent()) {
            return Optional.absent();
        }
        return Optional.of((SymRef) ref.get());
    }

}
