package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.storage.DeduplicationService;
import org.geogit.storage.Deduplicator;

import com.google.inject.Inject;

public class CreateDeduplicator extends AbstractGeoGitOp<Deduplicator> {
    private final DeduplicationService deduplicationService;
    
    @Inject
    public CreateDeduplicator(DeduplicationService deduplicationService) {
        this.deduplicationService = deduplicationService;
    }

    @Override
    public Deduplicator call() {
        return deduplicationService.createDeduplicator();
    }
}
