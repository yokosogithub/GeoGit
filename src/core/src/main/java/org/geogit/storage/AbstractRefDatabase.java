package org.geogit.storage;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * Provides a base implementation for different representations of the {@link RefDatabase}.
 * 
 * @see RefDatabase
 */
public abstract class AbstractRefDatabase implements RefDatabase {

    Lock lock = new ReentrantLock();

    /**
     * Locks access to the main repository refs.
     * 
     * @throws TimeoutException
     */
    @Override
    public final void lock() throws TimeoutException {
        try {
            if (!lock.tryLock(30, TimeUnit.SECONDS)) {
                throw new TimeoutException("The attempt to lock the database timed out.");
            }
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
    }

    /**
     * Unlocks access to the main repository refs.
     */
    @Override
    public final void unlock() {
        lock.unlock();
    }

    /**
     * @return all references under the specified prefix
     */
    @Override
    public Map<String, String> getAll(final String prefix) {
        Map<String, String> all = getAll();
        Predicate<String> keyPredicate = new Predicate<String>() {
            @Override
            public boolean apply(String refName) {
                return refName.startsWith(prefix);
            }
        };
        Map<String, String> filtered = Maps.filterKeys(all, keyPredicate);
        return filtered;
    }

}
