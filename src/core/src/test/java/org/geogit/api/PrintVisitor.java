/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import java.io.PrintWriter;

import org.geogit.repository.Repository;

/**
 * Helper class to visit a tree and print its structure to aid in debugging
 * 
 */
public class PrintVisitor implements TreeVisitor {

    private final PrintWriter writer;

    private int depth;

    private Repository repo;

    public PrintVisitor(Repository repo, PrintWriter writer) {
        this(repo, writer, 0);
    }

    public PrintVisitor(Repository repo, PrintWriter writer, int depth) {
        this.repo = repo;
        this.writer = writer;
        this.depth = depth;
    }

    @Override
    public boolean visitEntry(NodeRef ref) {
        indent();
        println(ref.toString());
        if (ref.getType().equals(RevObject.TYPE.TREE)) {
            RevTree tree = repo.getTree(ref.getObjectId());
            tree.accept(new PrintVisitor(repo, writer, depth + 1));
        }
        return true;
    }

    @Override
    public boolean visitSubTree(int bucket, ObjectId treeId) {
        return true;
    }

    private void println(char c) {
        writer.println(c);
        writer.flush();
    }

    private void println(String string) {
        writer.println(string);
        writer.flush();
    }

    private void print(String string) {
        writer.print(string);
        writer.flush();
    }

    private void indent() {
        for (int i = 0; i < depth; i++) {
            print('\t');
        }
    }

    private void print(char c) {
        writer.print(c);
        writer.flush();
    }
}