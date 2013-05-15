package org.geogit.storage;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.geogit.api.GeogitTransaction;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.TransactionBegin;
import org.geogit.api.plumbing.TransactionEnd;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.repository.StagingArea;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;

/**
 * A {@link StagingArea} decorator for a specific {@link GeogitTransaction transaction}.
 * <p>
 * This decorator creates a transaction specific namespace under the
 * {@code transactions/<transaction id>} path, and maps all query and storage methods to that
 * namespace.
 * 
 * @see GeogitTransaction
 * @see TransactionBegin
 * @see TransactionEnd
 */
public class TransactionStagingArea implements StagingArea {

    private StagingArea index;

    private StagingDatabase database;

    public TransactionStagingArea(final StagingArea index, final UUID transactionId) {
        this.index = index;
        database = new TransactionStagingDatabase(index.getDatabase(), transactionId);
    }

    @Override
    public StagingDatabase getDatabase() {
        return database;
    }

    @Override
    public void updateStageHead(ObjectId newTree) {
        index.updateStageHead(newTree);
    }

    @Override
    public RevTree getTree() {
        return index.getTree();
    }

    @Override
    public Optional<Node> findStaged(String path) {
        return index.findStaged(path);
    }

    @Override
    public void stage(ProgressListener progress, Iterator<DiffEntry> unstaged, long numChanges) {
        index.stage(progress, unstaged, numChanges);
    }

    @Override
    public Iterator<DiffEntry> getStaged(@Nullable List<String> pathFilters) {
        return index.getStaged(pathFilters);
    }

    @Override
    public long countStaged(@Nullable List<String> pathFilters) {
        return index.countStaged(pathFilters);
    }

    @Override
    public int countConflicted(@Nullable String pathFilter) {
        return database.getConflicts(null, pathFilter).size();
    }

    @Override
    public List<Conflict> getConflicted(@Nullable String pathFilter) {
        return database.getConflicts(null, pathFilter);
    }

}
