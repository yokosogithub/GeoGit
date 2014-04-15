/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import java.util.Iterator;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.repository.Repository;

import com.google.common.collect.AbstractIterator;

/**
 * An iterator that copies all new objects from a source repository to a destination repository.
 */
class LocalCopyingDiffIterator extends AbstractIterator<DiffEntry> {

    private Iterator<DiffEntry> source;

    private Repository sourceRepo;

    private Repository destinationRepo;

    /**
     * Constructs a new {@code LocalCopyingDiffIterator}.
     * 
     * @param source the {@link DiffEntry} iterator
     * @param sourceRepo the source repository
     * @param destinationRepo the destination repository
     */
    public LocalCopyingDiffIterator(Iterator<DiffEntry> source, Repository sourceRepo,
            Repository destinationRepo) {
        this.source = source;
        this.sourceRepo = sourceRepo;
        this.destinationRepo = destinationRepo;
    }

    /**
     * @return the next {@link DiffEntry}
     */
    protected DiffEntry computeNext() {
        if (source.hasNext()) {
            DiffEntry next = source.next();
            if (next.getNewObject() != null) {
                NodeRef newObject = next.getNewObject();
                RevObject object = sourceRepo.command(RevObjectParse.class)
                        .setObjectId(newObject.getNode().getObjectId()).call().get();

                RevObject metadata = null;
                if (newObject.getMetadataId() != ObjectId.NULL) {
                    metadata = sourceRepo.command(RevObjectParse.class)
                            .setObjectId(newObject.getMetadataId()).call().get();
                }

                if (!destinationRepo.blobExists(object.getId())) {
                    destinationRepo.getObjectDatabase().put(object);
                }
                if (metadata != null && !destinationRepo.blobExists(metadata.getId())) {
                    destinationRepo.getObjectDatabase().put(metadata);
                }
            }
            return next;
        }
        return endOfData();
    }
}
