package org.geogit.api.plumbing.diff;

import java.util.List;

import org.geogit.api.plumbing.merge.Conflict;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ConflictsReport {

    List<Conflict> conflicts;

    List<DiffEntry> unconflicted;

    public ConflictsReport() {
        conflicts = Lists.newArrayList();
        unconflicted = Lists.newArrayList();
    }

    public void addConflict(Conflict conflict) {
        conflicts.add(conflict);

    }

    public void addUnconflicted(DiffEntry diff) {
        unconflicted.add(diff);
    }

    public List<Conflict> getConflicts() {
        return ImmutableList.copyOf(conflicts);
    }

    public List<DiffEntry> getUnconflicted() {
        return ImmutableList.copyOf(unconflicted);
    }
}
