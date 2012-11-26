/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.LogOp;
import org.geogit.repository.Repository;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;

/**
 * Finds the common {@link RevCommit commit} ancestor of two commits.
 */
public class FindCommonAncestor extends AbstractGeoGitOp<Optional<RevCommit>> {

    private RevCommit left;

    private RevCommit right;

    private Repository repository;

    /**
     * Construct a new {@code FindCommonAncestor} using the specified {@link Repository}.
     * 
     * @param repository the repository
     */
    @Inject
    public FindCommonAncestor(Repository repository) {
        this.repository = repository;
    }

    /**
     * @param left the left {@link RevCommit}
     */
    public FindCommonAncestor setLeft(RevCommit left) {
        this.left = left;
        return this;
    }

    /**
     * @param right the right {@link RevCommit}
     */
    public FindCommonAncestor setRight(RevCommit right) {
        this.right = right;
        return this;
    }

    /**
     * Finds the common {@link RevCommit commit} ancestor of two commits.
     * 
     * @return an {@link Optional} of the ancestor commit, or {@link Optional#absent()} if no common
     *         ancestor was found
     */
    @Override
    public Optional<RevCommit> call() {
        Preconditions.checkState(left != null, "Left commit has not been set.");
        Preconditions.checkState(right != null, "Right commit has not been set.");

        if (left.getId().equals(right.getId())) {
            // They are the same commit
            return Optional.of(left);
        }

        getProgressListener().started();

        final int partitionSize;
        {
            final String key = "plumbing.partitionSize";
            partitionSize = Integer.parseInt(command(ConfigOp.class)
                    .setAction(ConfigAction.CONFIG_GET).setName(key).call()
                    .or(Collections.singletonMap(key, "1000")).get(key));
        }

        Iterator<RevCommit> log = command(LogOp.class).setUntil(right.getId()).call();

        Iterator<List<RevCommit>> partitions = Iterators.partition(log, partitionSize);

        getProgressListener().progress(50.f);

        while (partitions.hasNext()) {

            Set<ObjectId> ancestrySet = new HashSet<ObjectId>();
            populateSet(partitions.next(), ancestrySet);
            Optional<RevCommit> ancestor = findAncestor(left, ancestrySet);
            if (ancestor.isPresent()) {
                getProgressListener().complete();
                return ancestor;
            }
        }

        getProgressListener().complete();

        return Optional.absent();
    }

    private void populateSet(List<RevCommit> commits, Set<ObjectId> ancestrySet) {
        for (int i = 0; i < commits.size(); i++) {
            ancestrySet.add(commits.get(i).getId());
        }
    }

    private Optional<RevCommit> findAncestor(RevCommit descendant, Set<ObjectId> ancestrySet) {
        // Perform a breadth-first search of the graph, looking for a common ancestor
        Queue<RevCommit> commitQueue = new LinkedList<RevCommit>();
        commitQueue.add(descendant);
        while (!commitQueue.isEmpty()) {
            RevCommit commit = (RevCommit) commitQueue.remove();

            if (ancestrySet.contains(commit.getId())) {
                return Optional.of(commit);
            }

            for (ObjectId parent : commit.getParentIds()) {
                commitQueue.add(repository.getCommit(parent));
            }
        }

        return Optional.absent();
    }
}
