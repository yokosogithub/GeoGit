/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevPerson;

import com.google.common.base.Preconditions;

/**
 * Provides a method for constructing a {@link RevCommit} with the given parameters.
 * 
 * @see RevCommit
 */
public final class CommitBuilder {

    private ObjectId treeId;

    private List<ObjectId> parentIds;

    private String author;

    private String authorEmail;

    private String committer;

    private String committerEmail;

    private String message;

    private long timestamp;

    /**
     * @return the treeId of the commit
     */
    public ObjectId getTreeId() {
        return treeId;
    }

    /**
     * @param treeId the treeId to set
     */
    public void setTreeId(ObjectId treeId) {
        this.treeId = treeId;
    }

    /**
     * @return the parent commit {@link ObjectId ids}
     */
    public List<ObjectId> getParentIds() {
        return parentIds;
    }

    /**
     * @param parentIds the parentIds to set
     */
    public void setParentIds(List<ObjectId> parentIds) {
        this.parentIds = parentIds;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @return the author's email
     */
    public String getAuthorEmail() {
        return authorEmail;
    }

    /**
     * @param author the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @param email the author's email to set
     */
    public void setAuthorEmail(String email) {
        this.authorEmail = email;
    }

    /**
     * @return the committer
     */
    public String getCommitter() {
        return committer;
    }

    /**
     * @return the committer's email
     */
    public String getCommitterEmail() {
        return committerEmail;
    }

    /**
     * @param committer the committer to set
     */
    public void setCommitter(String committer) {
        this.committer = committer;
    }

    /**
     * @param email the committer's email to set
     */
    public void setCommitterEmail(String email) {
        this.committerEmail = email;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp timestamp, in UTC, of the commit. Let it blank for the builder to auto-set
     *        it at {@link #build(ObjectId)} time
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Builds the {@link RevCommit}.
     * 
     * @param id the id of the tree to build the commit off of
     * @return the constructed {@code RevCommit}
     */
    public RevCommit build(final ObjectId id) {
        Preconditions.checkNotNull(id, "Id can't be null");

        if (treeId == null) {
            throw new IllegalStateException("No tree id set");
        }
        return new RevCommit(id, treeId, parentIds, new RevPerson(author, authorEmail),
                new RevPerson(committer, committerEmail), message, getTimestamp());
    }
}
