/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.blongo;

import java.util.Iterator;

class ConcatenatedIterable<T> implements Iterable<T> {
    private final Iterable<T> a;
    private final Iterable<T> b;

    public ConcatenatedIterable(Iterable<T> a, Iterable<T> b) {
        this.a = a;
        this.b = b;
    }

    public Iterator<T> iterator() {
        return new ConcatenatedIterator<T>(a.iterator(), b.iterator());
    }
}
