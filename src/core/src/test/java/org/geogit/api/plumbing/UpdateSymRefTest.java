/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class UpdateSymRefTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testConstructorAndMutators() throws Exception {

        insertAndAdd(points1);
        geogit.command(CommitOp.class).call();
        Ref branch = geogit.command(BranchCreateOp.class).setName("branch1").call();

        geogit.command(UpdateSymRef.class).setDelete(false).setName(Ref.HEAD)
                .setNewValue("refs/heads/branch1").setOldValue(Ref.MASTER)
                .setReason("this is a test").call();

        Optional<ObjectId> branchId = geogit.command(RevParse.class).setRefSpec(Ref.HEAD).call();
        assertTrue(branch.getObjectId().equals(branchId.get()));
    }

    @Test
    public void testNoName() {
        exception.expect(IllegalStateException.class);
        geogit.command(UpdateSymRef.class).call();
    }

    @Test
    public void testNoValue() {
        exception.expect(IllegalStateException.class);
        geogit.command(UpdateSymRef.class).setName(Ref.HEAD).call();
    }

    @Test
    public void testDeleteRefTurnedIntoASymbolicRef() throws Exception {
        insertAndAdd(points1);
        RevCommit commit = geogit.command(CommitOp.class).call();
        Ref branch = geogit.command(BranchCreateOp.class).setName("branch1").call();

        assertTrue(branch.getObjectId().equals(commit.getId()));

        geogit.command(UpdateSymRef.class).setName("refs/heads/branch1")
                .setOldValue(commit.getId().toString()).setNewValue(Ref.MASTER)
                .setReason("this is a test").call();

        Optional<Ref> branchId = geogit.command(RefParse.class).setName("refs/heads/branch1")
                .call();

        assertTrue(((SymRef) branchId.get()).getTarget().equals(Ref.MASTER));

        geogit.command(UpdateSymRef.class).setName("refs/heads/branch1").setDelete(true).call();
    }

    @Test
    public void testDeleteRefThatDoesNotExist() {
        Optional<Ref> test = geogit.command(UpdateSymRef.class).setName("NoRef").setDelete(true)
                .call();
        assertFalse(test.isPresent());
    }
}
