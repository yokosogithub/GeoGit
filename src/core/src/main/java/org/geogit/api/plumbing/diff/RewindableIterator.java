/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing.diff;

import java.util.Iterator;
import java.util.LinkedList;

import com.google.common.collect.AbstractIterator;

class RewindableIterator<T> extends AbstractIterator<T> {

    private Iterator<T> subject;

    private LinkedList<T> returnQueue;

    public RewindableIterator(Iterator<T> subject) {
        this.subject = subject;
        this.returnQueue = new LinkedList<T>();
    }

    public void returnElement(T element) {
        this.returnQueue.offer(element);
    }

    @Override
    protected T computeNext() {
        T peak = returnQueue.poll();
        if (peak != null) {
            return peak;
        }
        if (!subject.hasNext()) {
            return endOfData();
        }
        return subject.next();
    }

}