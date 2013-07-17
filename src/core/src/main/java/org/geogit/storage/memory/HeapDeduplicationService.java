package org.geogit.storage.memory;

import org.geogit.storage.DeduplicationService;
import org.geogit.storage.Deduplicator;

public class HeapDeduplicationService implements DeduplicationService {
    @Override
    public Deduplicator createDeduplicator() {
        return new HeapDeduplicator();
    }
}
