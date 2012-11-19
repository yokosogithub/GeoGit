/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.geogit.api.NodeRef.appendChild;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.ResetOp;
import org.geogit.api.porcelain.ResetOp.ResetMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

public class ResetOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        // These values should be used during a commit to set author/committer
        // TODO: author/committer roles need to be defined better, but for
        // now they are the same thing.
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("groldan").call();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("groldan@opengeo.org").call();
    }

    @Test
    public void testResetAllMixed() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        geogit.command(ResetOp.class).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertFalse(repo.getIndex().findStaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(repo.getIndex().findStaged(appendChild(pointsName, idP3)).isPresent());

        assertEquals(oId1_modified,
                repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP1)).get()
                        .getObjectId());
        assertEquals(oId2, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

    }

    @Test
    public void testResetPoints() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);
        ObjectId oId4 = insertAndAdd(lines1);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
        assertEquals(oId4, repo.getIndex().findStaged(appendChild(linesName, idL1)).get()
                .getObjectId());

        geogit.command(ResetOp.class).addPattern(pointsName).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertFalse(repo.getIndex().findStaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(repo.getIndex().findStaged(appendChild(pointsName, idP3)).isPresent());
        assertTrue(repo.getIndex().findStaged(appendChild(linesName, idL1)).isPresent());

        assertEquals(oId1_modified,
                repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP1)).get()
                        .getObjectId());
        assertEquals(oId2, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
        assertEquals(oId4, repo.getWorkingTree().findUnstaged(appendChild(linesName, idL1)).get()
                .getObjectId());

    }

    @Test
    public void testResetSingle() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        geogit.command(ResetOp.class).addPattern(appendChild(pointsName, idP2)).call();

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertFalse(repo.getIndex().findStaged(appendChild(pointsName, idP2)).isPresent());
        assertTrue(repo.getIndex().findStaged(appendChild(pointsName, idP3)).isPresent());

        assertEquals(oId1_modified,
                repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP1)).get()
                        .getObjectId());
        assertEquals(oId2, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());
    }

    @Test
    public void testResetHard() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        geogit.command(ResetOp.class).setMode(ResetMode.HARD).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertFalse(repo.getIndex().findStaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(repo.getIndex().findStaged(appendChild(pointsName, idP3)).isPresent());

        assertEquals(oId1, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertFalse(repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP2)).isPresent());
        assertFalse(repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP3)).isPresent());

    }

    @Test
    public void testResetSoft() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        final Optional<Ref> currHead = geogit.command(RefParse.class).setName(Ref.HEAD).call();

        geogit.command(ResetOp.class).setCommit(Suppliers.ofInstance(currHead.get().getObjectId()))
                .setMode(ResetMode.SOFT).call();

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        assertEquals(oId1_modified,
                repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP1)).get()
                        .getObjectId());
        assertEquals(oId2, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getWorkingTree().findUnstaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

    }

    @Test
    public void testResetModePlusPatterns() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        exception.expect(IllegalStateException.class);
        geogit.command(ResetOp.class).addPattern(pointsName).setMode(ResetMode.SOFT).call();

    }

    @Test
    public void testResetMerge() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        exception.expect(UnsupportedOperationException.class);
        geogit.command(ResetOp.class).setMode(ResetMode.MERGE).call();

    }

    @Test
    public void testResetKeep() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        assertEquals(oId1, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());

        ObjectId oId1_modified = insertAndAdd(points1_modified);
        ObjectId oId2 = insertAndAdd(points2);
        ObjectId oId3 = insertAndAdd(points3);

        assertEquals(oId1_modified, repo.getIndex().findStaged(appendChild(pointsName, idP1)).get()
                .getObjectId());
        assertEquals(oId2, repo.getIndex().findStaged(appendChild(pointsName, idP2)).get()
                .getObjectId());
        assertEquals(oId3, repo.getIndex().findStaged(appendChild(pointsName, idP3)).get()
                .getObjectId());

        exception.expect(UnsupportedOperationException.class);
        geogit.command(ResetOp.class).setCommit(null).setMode(ResetMode.KEEP).call();

    }

    @Test
    public void testResetNoCommits() throws Exception {
        exception.expect(IllegalStateException.class);
        geogit.command(ResetOp.class).call();

    }

}
