/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Collections;

import org.geogit.api.RevObject.TYPE;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class RevCommitTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testConstructorAndAccessors() {
        RevPerson committer = new RevPerson("ksishmael", "kelsey.ishmael@lmnsolutions.com", 12345,
                12345);
        RevPerson author = new RevPerson("test", "test@email.com", 12345, 12345);
        ObjectId id = ObjectId.forString("new commit");
        ObjectId treeId = ObjectId.forString("test tree");
        String message = "This is a test commit";
        ImmutableList<ObjectId> parentIds = ImmutableList.of(ObjectId.forString("Parent 1"));
        RevCommit commit = new RevCommit(id, treeId, parentIds, author, committer, message);

        assertEquals(committer, commit.getCommitter());
        assertEquals(author, commit.getAuthor());
        assertEquals(id, commit.getId());
        assertEquals(treeId, commit.getTreeId());
        assertEquals(message, commit.getMessage());
        assertEquals(parentIds, commit.getParentIds());
        assertEquals(TYPE.COMMIT, commit.getType());
        assertEquals(parentIds.get(0), commit.parentN(0).get());

        parentIds = ImmutableList.of();
        commit = new RevCommit(id, treeId, parentIds, author, committer, message);
        assertEquals(Collections.EMPTY_LIST, commit.getParentIds());
        assertEquals(Optional.absent(), commit.parentN(0));
    }

    @Test
    public void testToStringAndEquals() {
        RevPerson committer = new RevPerson("ksishmael", "kelsey.ishmael@lmnsolutions.com", 12345,
                12345);
        RevPerson author = new RevPerson("test", "test@email.com", 12345, 12345);
        ObjectId id = ObjectId.forString("new commit");
        ObjectId treeId = ObjectId.forString("test tree");
        String message = "This is a test commit";
        ImmutableList<ObjectId> parentId = ImmutableList.of(ObjectId.forString("Parent 1"));
        ImmutableList<ObjectId> emptyParentIds = ImmutableList.of();
        RevCommit commit = new RevCommit(id, treeId, parentId, author, committer, message);

        String commitString = commit.toString();

        assertEquals("Commit[" + id.toString() + ", '" + message + "']", commitString);

        RevCommit commit2 = new RevCommit(ObjectId.forString("second commit"), treeId, parentId,
                author, committer, message);

        assertTrue(commit.equals(commit2));

        commit2 = new RevCommit(id, ObjectId.forString("new test tree"), parentId, author,
                committer, message);

        assertFalse(commit.equals(commit2));

        commit2 = new RevCommit(id, treeId, emptyParentIds, author, committer, message);

        assertFalse(commit.equals(commit2));

        commit2 = new RevCommit(id, treeId, parentId, committer, committer, message);

        assertFalse(commit.equals(commit2));

        commit2 = new RevCommit(id, treeId, parentId, author, author, message);

        assertFalse(commit.equals(commit2));

        commit2 = new RevCommit(id, treeId, parentId, author, committer, "new message");

        assertFalse(commit.equals(commit2));

        assertFalse(commit.equals(author));

        assertTrue(commit.equals(commit));
    }

}
