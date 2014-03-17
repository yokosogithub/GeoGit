/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.blongo;

import java.util.Iterator;

abstract class AdaptingIterable<From,To> implements Iterable<To> {
    private final Iterable<From> delegate;

    public AdaptingIterable(Iterable<From> source) {
        this.delegate = source;
    }

    public Iterator<To> iterator() {
        return new AdaptingIterator<From,To>(delegate.iterator()) {
            public To transform(From from) {
                return AdaptingIterable.this.transform(from);
            }
        };
    }

    abstract To transform(From from);
}
