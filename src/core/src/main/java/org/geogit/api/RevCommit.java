/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * A reference to a commit in the DAG.
 * 
 */
public class RevCommit extends AbstractRevObject {

    private ObjectId treeId;

    private ImmutableList<ObjectId> parentIds;

    private RevPerson author;

    private RevPerson committer;

    private String message;

    /**
     * Constructs a new {@code RevCommit} with the given {@link ObjectId}.
     * 
     * @param id the object id to use
     */
    public RevCommit(final ObjectId id) {
        super(id);
    }

    @Override
    public TYPE getType() {
        return TYPE.COMMIT;
    }

    /**
     * Constructs a new {@code RevCommit} with the given parameters.
     * 
     * @param id the {@link ObjectId} to use
     * @param treeId the {@link ObjectId} of the tree this commit points to
     * @param parentIds a list of parent commits' {@link ObjectId}s
     * @param author the author of this commit
     * @param committer the committer of this commit
     * @param message the message for this commit
     */
    public RevCommit(final ObjectId id, ObjectId treeId, ImmutableList<ObjectId> parentIds,
            RevPerson author, RevPerson committer, String message) {
        this(id);
        checkNotNull(treeId);
        checkNotNull(parentIds);
        checkNotNull(author);
        checkNotNull(committer);
        checkNotNull(message);
        this.treeId = treeId;
        this.parentIds = parentIds;
        this.author = author;
        this.committer = committer;
        this.message = message;
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
    public ImmutableList<ObjectId> getParentIds() {
        return this.parentIds;
    }

    /**
     * Short cut for {@code getParentIds().get(parentIndex)}.
     * <p>
     * Beware {@code parentIndex} is <b>zero-based</b>, whilst the command line interface syntax for
     * parents is one-based (e.g. {@code <commit id>^1} for the first parent instead of
     * {@code <commit id>^0}).
     * 
     * @param parentIndex
     * @return the parent id at the given index, or absent
     */
    public Optional<ObjectId> parentN(int parentIndex) {
        Optional<ObjectId> parent = Optional.absent();
        if (parentIds.size() > parentIndex) {
            parent = Optional.of(parentIds.get(parentIndex));
        }
        return parent;
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
     * @return the {@code RevCommit} as a readable string
     */
    @Override
    public String toString() {
        return "Commit[" + getId() + ", '" + message + "']";
    }

    /**
     * Equality is based on author, committer, message, parent ids, and tree id.
     * 
     * @see AbstractRevObject#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RevCommit) && !super.equals(o)) {
            return false;
        }
        RevCommit c = (RevCommit) o;
        return equal(getAuthor(), c.getAuthor()) && equal(getCommitter(), c.getCommitter())
                && equal(getMessage(), c.getMessage()) && equal(getParentIds(), c.getParentIds())
                && equal(getTreeId(), c.getTreeId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), getTreeId(), getParentIds(), getAuthor(), getCommitter(),
                getMessage());
    }
}
