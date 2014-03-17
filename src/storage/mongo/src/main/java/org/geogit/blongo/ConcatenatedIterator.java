/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.blongo;

import java.util.Iterator;
import java.util.NoSuchElementException;

class ConcatenatedIterator<T> implements Iterator<T> {
    private final Iterator<T>[] chain;
    private int index = 0;

    public ConcatenatedIterator(Iterator<T>... iterators) {
        this.chain = iterators;
    }

    public boolean hasNext() {
        while (index < chain.length && !chain[index].hasNext()) {
            index = index + 1;
        }
        return index < chain.length;
    }

    public T next() {
        while (index < chain.length && !chain[index].hasNext()) {
            index = index + 1;
        }
        if (index < chain.length) {
            return chain[index].next();
        } else {
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
