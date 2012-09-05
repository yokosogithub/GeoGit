/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.merge.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.DiffOp;
import org.geogit.api.LogOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RebaseOp;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.merge.AbstractMergeOp;
import org.geogit.api.merge.MergeResult;
import org.geogit.api.merge.MergeUtils;
import org.geogit.repository.CommitBuilder;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectInserter;

import com.google.common.collect.Iterators;

/**
 * <p>
 * Very simple merge; to push the HEAD up to the current remotes head.
 * </p>
 * <p>
 * This will rebase if there are no new commits on this repo. Otherwise it will create a new commit
 * head and set the parents the old head and the branch head.
 * </p>
 * 
 * @author jhudson
 * @since 1.2.0
 */
public class ReverseRebaseMergeOp extends AbstractMergeOp {

    public ReverseRebaseMergeOp() {
        super();
    }

    public MergeResult call() throws Exception {
        MergeResult mergeResult = new MergeResult();

        /*
         * just in case
         */
        if (branch == null) {
            return mergeResult;
        }

        /*
         * Grab old head - this will be used to find all the new commits above what our current head
         * is.
         */
        RevCommit oldHead;

        final Repository repository = getRepository();
        Ref head = repository.getHead();
        if (!ObjectId.NULL.equals(head.getObjectId())) {
            oldHead = repository.getCommit(repository.getHead().getObjectId());
        } else {
            /*
             * current head is 000...000 so just grab the top commit - its now the index. rebase to
             * it
             */
            rebase();
            return mergeResult;
        }

        /*
         * Work out if this is a rebase or a merge
         */
        LogOp l = new LogOp(repository);
        Iterator<RevCommit> s = l.setSince(oldHead.getId()).call();

        if (Iterators.contains(s, oldHead)) { /* rebase */
            rebase();
        } else { /* merge - new commit head and add parents of both branches */
            /*
             * New head
             */
            final ObjectId commitId;
            final RevCommit branchHead;
            {
                /*
                 * Grab branch head parents
                 */
                branchHead = repository.getCommit(branch.getObjectId());

                /*
                 * Grab the branch split
                 */
                RevCommit branchSplit = MergeUtils.findBranchCommitSplit(branchHead, repository);

                /*
                 * Set the parents to the current master head commit - thus moving it to 'above' the
                 * head
                 */
                List<ObjectId> newParents = new ArrayList<ObjectId>();
                newParents.add(oldHead.getId());
                branchSplit.setParentIds(newParents);

                CommitBuilder cb = new CommitBuilder();

                /*
                 * Merge the trees
                 */
                ObjectId treeId = mergeTrees(oldHead.getId(), branchHead.getId());

                /*
                 * add the parents
                 */
                List<ObjectId> parents = Arrays.asList(branchHead.getId());
                cb.setParentIds(parents);
                cb.setTreeId(treeId);
                cb.setMessage(this.comment);

                /*
                 * insert the new commit
                 */
                ObjectInserter objectInserter = repository.newObjectInserter();
                commitId = objectInserter
                        .insert(repository.newCommitWriter(cb.build(ObjectId.NULL)));
            }

            /*
             * Update the head
             */
            repository.getRefDatabase().put(new Ref(Ref.HEAD, commitId, TYPE.COMMIT));

            /*
             * diff the changes
             */
            DiffOp diffOp = new DiffOp(repository);
            Iterator<DiffEntry> diffs = diffOp.setNewVersion(oldHead.getId())
                    .setOldVersion(branchHead.getId()).call();

            while (diffs.hasNext()) {
                DiffEntry diff = diffs.next();
                /*
                 * This might be a little over zealous - what about ChangeType.ADD and
                 * ChangeType.DELETE?
                 */
                if (diff.getType() == ChangeType.MODIFY) {
                    mergeResult.addDiff(diff);
                }
            }

            LOGGER.info("Merged master -> " + branch.getName());
            LOGGER.info(" " + commitId.printSmallId());
        }

        return mergeResult;
    }

    /**
     * Merge the two trees together so the new commit has a reference to the actual features
     * 
     * TODO: is this actually needed? GIT uses its history to traverse its commits to create its
     * checkout - since the parents of this new commit HEAD have the trees should this BE
     * objectId.NULL... not sure?
     * 
     * @param oldHead
     * @param branchHead
     * @return ObjectId of the new tree created and inserted into the DB
     * @throws Exception
     */
    private ObjectId mergeTrees(ObjectId oldHead, ObjectId branchHead) throws Exception {
        return ObjectId.NULL;
    }

    /**
     * Point the HEAD at the current remote branch head - is this a rebaseOp?
     * 
     * @throws Exception
     */
    private void rebase() throws Exception {
        RebaseOp rebaseOp = new RebaseOp(getRepository());
        rebaseOp.include(branch).call();
    }
}