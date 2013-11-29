package org.geogit.osm.internal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;

@ThreadSafe
public class QueueIterator<T> extends AbstractIterator<T> {

    private BlockingQueue<T> queue;

    private int timeout;

    private TimeUnit timeoutUnit;

    private volatile boolean finish;

    public QueueIterator(int queueCapacity, int timeout, TimeUnit timeoutUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        queue = new ArrayBlockingQueue<T>(queueCapacity);
    }

    public void finish() {
        this.finish = true;
    }

    public void put(T elem) {
        try {
            while (!finish && !queue.offer(elem, timeout, timeoutUnit)) {
                System.err.println("queue.offer timed out after " + timeout + " " + timeoutUnit
                        + ". retrying...");
            }
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected T computeNext() {
        try {
            T next = null;
            while (!finish && (next = queue.poll(timeout, timeoutUnit)) == null) {
                System.err.println("queue.poll timed out after " + timeout + " " + timeoutUnit
                        + ". retrying...");
            }
            if (next == null) {
                return endOfData();
            }
            return next;
        } catch (InterruptedException e) {
            return endOfData();
        }
    }

}
