/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.List;

import org.geogit.api.plumbing.HashObject;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public final class CommitBuilder {

    private ObjectId treeId;

    private List<ObjectId> parentIds;

    private String author;

    private String authorEmail;

    private String committer;

    private String committerEmail;

    private String message;

    private long timestamp;

    public CommitBuilder() {
        parentIds = ImmutableList.of();
    }

    /**
     * @param copy the commit to initialize this builder properties with
     */
    public CommitBuilder(RevCommit copy) {
        setAuthor(copy.getAuthor().getName());
        setAuthorEmail(copy.getAuthor().getEmail());
        setCommitter(copy.getCommitter().getName());
        setCommitterEmail(copy.getCommitter().getEmail());
        setMessage(copy.getMessage());
        setParentIds(copy.getParentIds());
        setTreeId(copy.getTreeId());
        setTimestamp(copy.getTimestamp());
    }

    /**
     * @return the treeId of the commit
     */
    public ObjectId getTreeId() {
        return treeId;
    }

    /**
     * @param treeId the treeId to set
     */
    public CommitBuilder setTreeId(ObjectId treeId) {
        this.treeId = treeId;
        return this;
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
    @SuppressWarnings("unchecked")
    public CommitBuilder setParentIds(List<ObjectId> parentIds) {
        this.parentIds = (List<ObjectId>) (parentIds == null ? ImmutableList.of() : ImmutableList
                .copyOf(parentIds));
        return this;
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
    public CommitBuilder setAuthor(String author) {
        this.author = author;
        return this;
    }

    /**
     * @param email the author's email to set
     */
    public CommitBuilder setAuthorEmail(String email) {
        this.authorEmail = email;
        return this;
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
    public CommitBuilder setCommitter(String committer) {
        this.committer = committer;
        return this;
    }

    /**
     * @param email the committer's email to set
     */
    public CommitBuilder setCommitterEmail(String email) {
        this.committerEmail = email;
        return this;
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
    public CommitBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp timestamp, in UTC, of the commit. Let it blank for the builder to auto-set
     *        it at {@link #build()} time
     */
    public CommitBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public RevCommit build() {
        if (treeId == null) {
            throw new IllegalStateException("No tree id set");
        }

        final ObjectId treeId = this.treeId;
        final List<ObjectId> parentIds = this.parentIds;
        final RevPerson author = new RevPerson(this.author, authorEmail);
        final RevPerson committer = new RevPerson(this.committer, committerEmail);
        final long timestamp = getTimestamp();
        final String commitMessage = this.message == null ? "" : this.message;

        RevCommit unnnamedCommit = new RevCommit(ObjectId.NULL, treeId, parentIds, author,
                committer, commitMessage, timestamp);
        ObjectId commitId = new HashObject().setObject(unnnamedCommit).call();

        return new RevCommit(commitId, treeId, parentIds, author, committer, commitMessage,
                timestamp);
    }

    /**
     * This method is deprecated and should be removed as soon as possible. The only reason to
     * preserve it is that it _seems_ the hessian commit reader/writers don't produce always the
     * same contents, and worse, seems to be random. See comments in HessianCommitReader
     * 
     * @deprecated use {@link #build()}
     */
    @Deprecated
    public RevCommit build(ObjectId id) {
        Preconditions.checkNotNull(id);
        if (treeId == null) {
            throw new IllegalStateException("No tree id set");
        }

        final ObjectId treeId = this.treeId;
        final List<ObjectId> parentIds = this.parentIds;
        final RevPerson author = new RevPerson(this.author, authorEmail);
        final RevPerson committer = new RevPerson(this.committer, committerEmail);
        final long timestamp = getTimestamp();
        final String commitMessage = this.message;

        return new RevCommit(id, treeId, parentIds, author, committer, commitMessage, timestamp);
    }
}
