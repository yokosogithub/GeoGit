/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.Iterator;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

public interface RevTree extends RevObject {

    public static RevTree NULL = new RevTree() {

        @Override
        public TYPE getType() {
            return TYPE.TREE;
        }

        @Override
        public ObjectId getId() {
            return ObjectId.NULL;
        }

        // @Override
        // public BigInteger size() {
        // return BigInteger.ZERO;
        // }

        @Override
        public MutableTree mutable() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<NodeRef> iterator(Predicate<NodeRef> filter) {
            return Iterators.emptyIterator();
        }

        @Override
        public boolean isNormalized() {
            return true;
        }

        @Override
        public Optional<NodeRef> get(String key) {
            return Optional.absent();
        }

        @Override
        public void accept(TreeVisitor visitor) {
            // nothing to do
        }
    };

    public abstract Optional<NodeRef> get(final String key);

    public abstract void accept(TreeVisitor visitor);

    // public abstract BigInteger size();

    public abstract Iterator<NodeRef> iterator(Predicate<NodeRef> filter);

    public abstract boolean isNormalized();

    public abstract MutableTree mutable() throws UnsupportedOperationException;
}