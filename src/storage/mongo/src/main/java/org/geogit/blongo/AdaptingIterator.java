/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.blongo;

import java.util.Iterator;

abstract class AdaptingIterator<From,To> implements Iterator<To> {
    private final Iterator<From> delegate;

    public AdaptingIterator(Iterator<From> source) {
        this.delegate = source;

    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public To next() {
        return transform(delegate.next());
    }

    public void remove() {
        delegate.remove();
    }

    public abstract To transform(From from);
}
