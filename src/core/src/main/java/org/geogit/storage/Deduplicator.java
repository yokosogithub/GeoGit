package org.geogit.storage;

import java.util.List;

import org.geogit.api.ObjectId;

public interface Deduplicator {
    boolean isDuplicate(ObjectId id);
    boolean visit(ObjectId id);
    void removeDuplicates(List<ObjectId> ids);
    void release();
}