package org.geogit.storage.memory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geogit.api.ObjectId;
import org.geogit.storage.Deduplicator;

public class HeapDeduplicator implements Deduplicator {
    private final Set<ObjectId> seen = new HashSet<ObjectId>();
    
    @Override
    public boolean visit(ObjectId id) {
        return !seen.add(id);
    }
    
    @Override
    public boolean isDuplicate(ObjectId id) {
        return seen.contains(id);
    }

    @Override
    public void removeDuplicates(List<ObjectId> ids) {
        ids.removeAll(seen);
    }

    @Override
    public void release() {
        // No-op
    }
}
