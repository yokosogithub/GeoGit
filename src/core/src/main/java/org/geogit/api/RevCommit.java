/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Objects.equal;

import java.util.Collections;
import java.util.List;

/**
 * A reference to a commit in the DAG.
 * 
 * @author groldan
 * 
 */
public class RevCommit extends AbstractRevObject {

    private ObjectId treeId;

    private List<ObjectId> parentIds;

    private RevPerson author;

    private RevPerson committer;

    private String message;

    private long timestamp;

    public RevCommit(final ObjectId id) {
        super(id, TYPE.COMMIT);
    }

    public RevCommit(final ObjectId id, ObjectId treeId, List<ObjectId> parentIds,
            RevPerson author, RevPerson committer, String message, long timestamp) {
        this(id);

        this.treeId = treeId;
        this.parentIds = parentIds;
        this.author = author;
        this.committer = committer;
        this.message = message;
        this.timestamp = timestamp;
    }

    /**
     * @return the id of the tree this commit points to
     */
    public ObjectId getTreeId() {
        return treeId;
    }

    /**
     * @return the parentIds
     */
    @SuppressWarnings("unchecked")
    public List<ObjectId> getParentIds() {
        return parentIds == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(parentIds);
    }

    /**
     * @return the author
     */
    public RevPerson getAuthor() {
        return author;
    }

    /**
     * @return the committer
     */
    public RevPerson getCommitter() {
        return committer;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the commit time stamp in UTC milliseconds
     * 
     * @return the commit's time stamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Commit[" + getId() + ", '" + message + "']";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RevCommit) && super.equals(o)) {
            return false;
        }
        RevCommit c = (RevCommit) o;
        return equal(getAuthor(), c.getAuthor()) && equal(getCommitter(), c.getCommitter())
                && equal(getMessage(), c.getMessage()) && equal(getParentIds(), c.getParentIds())
                && equal(getTimestamp(), c.getTimestamp()) && equal(getTreeId(), c.getTreeId());
    }
}
