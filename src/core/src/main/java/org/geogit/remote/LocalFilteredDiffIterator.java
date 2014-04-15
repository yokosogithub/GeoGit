/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import java.util.Iterator;

import org.geogit.api.ObjectId;
import org.geogit.api.RepositoryFilter;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.Repository;

/**
 * Overrides the basic implementation of {@link FilteredDiffIterator} by providing hints as to which
 * objects should be tracked, as well as copying affected features to the local repository.
 */
class LocalFilteredDiffIterator extends FilteredDiffIterator {

    private Repository destinationRepo;

    /**
     * Constructs a new {@code LocalFilteredDiffIterator}.
     * 
     * @param source the original iterator
     * @param sourceRepo the source full repository
     * @param destinationRepo the sparse repository
     * @param repoFilter the repository filter
     */
    public LocalFilteredDiffIterator(Iterator<DiffEntry> source, Repository sourceRepo,
            Repository destinationRepo, RepositoryFilter repoFilter) {
        super(source, sourceRepo, repoFilter);
        this.destinationRepo = destinationRepo;
    }

    /**
     * Hints that objects that I have in the sparse repository should continue to be tracked.
     * 
     * @param objectId the id of the object
     * @return true if the object should be tracked, false if it should only be tracked if it
     *         matches the filter
     */
    @Override
    protected boolean trackingObject(ObjectId objectId) {
        return destinationRepo.blobExists(objectId);
    }

    /**
     * Adds new objects that match my filter or were tracked to the sparse repository.
     * 
     * @param object the object to process
     */
    @Override
    protected void processObject(RevObject object) {
        if (object != null && !destinationRepo.blobExists(object.getId())) {
            destinationRepo.getIndex().getDatabase().put(object);
        }
    }

}
