/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.RevertOp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

public class RevertOpTest extends RepositoryTestCase {
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
    public void testRevert() throws Exception {
        ObjectId oId1 = insertAndAdd(points1);
        RevCommit c1 = geogit.command(CommitOp.class).setMessage("commit for " + idP1).call();

        insertAndAdd(points2);
        RevCommit c2 = geogit.command(CommitOp.class).setMessage("commit for " + idP2).call();

        insertAndAdd(points1_modified);
        RevCommit c3 = geogit.command(CommitOp.class).setMessage("commit for modified " + idP1)
                .call();

        ObjectId oId3 = insertAndAdd(points3);
        RevCommit c4 = geogit.command(CommitOp.class).setMessage("commit for " + idP3).call();

        deleteAndAdd(points3);
        RevCommit c5 = geogit.command(CommitOp.class).setMessage("commit for deleted " + idP3)
                .call();

        // revert Points.2 add, Points.1 change, and Points.3 delete
        geogit.command(RevertOp.class).addCommit(Suppliers.ofInstance(c2.getId()))
                .addCommit(Suppliers.ofInstance(c3.getId()))
                .addCommit(Suppliers.ofInstance(c5.getId())).call();

        final Optional<Ref> currHead = geogit.command(RefParse.class).setName(Ref.HEAD).call();

        final Optional<ObjectId> headTreeId = geogit.command(ResolveTreeish.class)
                .setTreeish(currHead.get().getObjectId()).call();

        RevTree headTree = repo.getTree(headTreeId.get());

        Optional<NodeRef> points1Node = geogit.command(FindTreeChild.class)
                .setChildPath(NodeRef.appendChild(pointsName, idP1)).setParent(headTree).call();

        assertTrue(points1Node.isPresent());
        assertTrue(points1Node.get().getNode().getObjectId().equals(oId1));

        Optional<NodeRef> points2Node = geogit.command(FindTreeChild.class)
                .setChildPath(NodeRef.appendChild(pointsName, idP2)).setParent(headTree).call();

        assertFalse(points2Node.isPresent());

        Optional<NodeRef> points3Node = geogit.command(FindTreeChild.class)
                .setChildPath(NodeRef.appendChild(pointsName, idP3)).setParent(headTree).call();

        assertTrue(points3Node.isPresent());
        assertTrue(points3Node.get().getNode().getObjectId().equals(oId3));

        Iterator<RevCommit> log = geogit.command(LogOp.class).call();

        // There should be 3 new commits, followed by all of the previous commits.
        log.next();
        log.next();
        log.next();

        assertTrue(log.next().getId().equals(c5.getId()));
        assertTrue(log.next().getId().equals(c4.getId()));
        assertTrue(log.next().getId().equals(c3.getId()));
        assertTrue(log.next().getId().equals(c2.getId()));
        assertTrue(log.next().getId().equals(c1.getId()));
    }
}
