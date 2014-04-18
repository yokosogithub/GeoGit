/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.Iterator;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;
import org.geogit.di.CanRunDuringConflict;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

@CanRunDuringConflict
public class StatusOp extends AbstractGeoGitOp<StatusOp.StatusSummary> {

    public static class StatusSummary {

        private static final Supplier<Iterator<DiffEntry>> empty;

        private static final Supplier<Iterable<Conflict>> no_conflicts;
        static {
            Iterator<DiffEntry> e = Iterators.<DiffEntry> emptyIterator();
            empty = Suppliers.ofInstance(e);
            Iterable<Conflict> c = ImmutableList.of();
            no_conflicts = Suppliers.ofInstance(c);
        }

        private Supplier<Iterable<Conflict>> conflicts = no_conflicts;

        private Supplier<Iterator<DiffEntry>> staged = empty;

        private Supplier<Iterator<DiffEntry>> unstaged = empty;

        private long countStaged, countUnstaged;

        private int countConflicted;

        public Supplier<Iterable<Conflict>> getConflicts() {
            return conflicts;
        }

        public Supplier<Iterator<DiffEntry>> getStaged() {
            return staged;
        }

        public Supplier<Iterator<DiffEntry>> getUnstaged() {
            return unstaged;
        }

        public long getCountStaged() {
            return countStaged;
        }

        public long getCountUnstaged() {
            return countUnstaged;
        }

        public int getCountConflicts() {
            return countConflicted;
        }
    }

    @Override
    public StatusSummary call() {
        WorkingTree workTree = getWorkTree();
        StagingArea index = getIndex();

        StatusSummary summary = new StatusSummary();

        summary.countStaged = index.countStaged(null).getCount();
        summary.countUnstaged = workTree.countUnstaged(null).getCount();
        summary.countConflicted = index.countConflicted(null);

        if (summary.countStaged > 0) {
            summary.staged = command(DiffIndex.class).setReportTrees(true);
        }
        if (summary.countUnstaged > 0) {
            summary.unstaged = command(DiffWorkTree.class).setReportTrees(true);
        }
        if (summary.countConflicted > 0) {
            summary.conflicts = command(ConflictsReadOp.class);
        }
        return summary;
    }
}
