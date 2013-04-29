package org.geogit.api;

public class Pair<F, S> {

    private final F first;

    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return first.hashCode() ^ second.hashCode();
    }

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