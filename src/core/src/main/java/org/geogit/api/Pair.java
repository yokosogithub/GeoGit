/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * Provides a basic implementation for a pair object.
 */
public class Pair<F, S> {

    private final F first;

    private final S second;

    /**
     * Constructs a new {@code Pair} with the given objects.
     * 
     * @param first the first object
     * @param second the second object
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * @return the first object
     */
    public F getFirst() {
        return first;
    }

    /**
     * @return the second object
     */
    public S getSecond() {
        return second;
    }

    /**
     * @return the hash code for this pair
     */
    @Override
    public int hashCode() {
        return first.hashCode() ^ second.hashCode();
    }

    /**
     * Compares this pair to another pair.
     * 
     * @param o the pair to compare to
     * @reutrn true if the pairs' objects are equal
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof Pair))
            return false;
        Pair<F, S> pair = (Pair<F, S>) o;
        return this.first.equals(pair.getFirst()) && this.second.equals(pair.getSecond());
    }

}