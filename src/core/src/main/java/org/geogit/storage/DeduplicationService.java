package org.geogit.storage;

/**
 * A service for providing deduplicators.
 */
public interface DeduplicationService {
    /**
     * Create a new Deduplicator.  Clients MUST ensure that the deduplicator's
     * release() method is called.  For example:
     *
     * <code>
     *   Deduplicator deduplicator = deduplicationService().createDeduplicator();
     *   try {
     *       client.use(deduplicator);
     *   } finally {
     *       deduplicator.release();
     *   }
     * </code>
     */
    Deduplicator createDeduplicator();
}
