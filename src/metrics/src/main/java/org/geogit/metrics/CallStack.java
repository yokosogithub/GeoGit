/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.metrics;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

final class CallStack {

    static final ThreadLocal<CallStack> CALL_STACK = new ThreadLocal<CallStack>();

    private boolean root;

    private String name;

    private long nanoTime = -1L;

    private boolean success;

    private ArrayList<CallStack> children;

    private CallStack(String name, boolean root) {
        this.name = name;
        this.root = root;
    }

    public static CallStack push(String name) {
        CallStack root = CALL_STACK.get();
        final CallStack newElement;
        if (root == null) {
            root = new CallStack(name, true);
            newElement = root;
            CALL_STACK.set(root);
        } else {
            CallStack current = root.current();
            newElement = current.add(name);
        }
        return newElement;
    }

    public static CallStack pop(long nanoTime, boolean success) {
        CallStack root = CALL_STACK.get();
        checkNotNull(root, "called pop with no prior push?");

        CallStack current = root.current();
        current.setTime(nanoTime, success);
        if (current.isRoot()) {
            CALL_STACK.remove();
        }

        return current;
    }

    private CallStack current() {
        return current(this);
    }

    private static CallStack current(CallStack parent) {
        if (parent.children == null) {
            return parent;
        }
        for (int i = parent.children.size() - 1; i >= 0; i--) {
            CallStack lastAlive = parent.children.get(i);
            if (!lastAlive.isFinished()) {
                return lastAlive.current();
            }
        }
        checkState(!parent.isFinished());
        return parent;
    }

    boolean isFinished() {
        return nanoTime != -1L;
    }

    private void setTime(long nanoTime, boolean success) {
        this.nanoTime = nanoTime;
        this.success = success;

    }

    private CallStack add(final String name) {
        CallStack cs = new CallStack(name, false);
        if (this.children == null) {
            this.children = Lists.newArrayListWithCapacity(4);
        }
        this.children.add(cs);
        return cs;
    }

    boolean isRoot() {
        return root;
    }

    public String getName() {
        return this.name;
    }

    public long getNanoTime() {
        return this.nanoTime;
    }

    public boolean isSuccess() {
        return this.success;
    }

    @Override
    public String toString() {
        return toString(TimeUnit.NANOSECONDS);
    }

    public String toString(TimeUnit durationUnit) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dump(new PrintStream(out), durationUnit);
        String string = out.toString();
        return string;
    }

    public String toString(final double durationFactor, final String unitName, final long totalNanos) {

        final double percent = (this.nanoTime * 100.0) / totalNanos;

        return String.format("%s -> %,.2f %s (%.2f%%), success: %s", name,
                (durationFactor * nanoTime), unitName, percent, success);
    }

    public void dump(PrintStream stream, TimeUnit durationUnit) {
        final double durationFactor = 1.0 / durationUnit.toNanos(1L);
        dump(stream, this, 0, durationFactor, durationUnit.name(), this.nanoTime);
        stream.flush();
    }

    private void dump(PrintStream out, final CallStack stackElement, final int indentLevel,
            final double durationFactor, final String unitName, final long totalNanos) {
        if (indentLevel > 0) {
            out.print(Strings.repeat("    ", indentLevel));
        }
        out.println(stackElement.toString(durationFactor, unitName, totalNanos));
        if (stackElement.children != null) {
            for (CallStack childElem : stackElement.children) {
                dump(out, childElem, indentLevel + 1, durationFactor, unitName, totalNanos);
            }
        }
    }

}
