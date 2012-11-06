/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.repository.Repository;

import com.google.common.base.Throwables;
import com.google.inject.Inject;

/**
 * Pulls the object with the given {@link ObjectId} from the repository and prints it to the given
 * {@link PrintStream}.
 * 
 * @see ObjectId
 * @see Repository
 * @see PrintStream
 */
public class ShowOp extends AbstractGeoGitOp<Void> {

    private PrintStream out;

    private ObjectId oid;

    private Repository repo;

    /**
     * Constructs a new {@code ShowOp} with the given repository.
     * 
     * @param repository the repository where the object is stored
     */
    @Inject
    public ShowOp(final Repository repository) {
        this.repo = repository;
        this.out = System.err;
    }

    /**
     * @param out the stream to print the object to
     * @return this
     */
    public ShowOp setPrintStream(final PrintStream out) {
        this.out = out;
        return this;
    }

    /**
     * @param oid the id for the object to print
     * @return this
     */
    public ShowOp setObjectId(final ObjectId oid) {
        this.oid = oid;
        return this;
    }

    /**
     * Executes the show operation.
     * 
     * @return {@code Void}
     */
    @Override
    public Void call() {
        try {
            final InputStream raw = repo.getRawObject(oid);
            final PrintStream out = this.out;
            try {
                repo.newBlobPrinter().print(raw, out);
            } finally {
                raw.close();
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return null;
    }

}
