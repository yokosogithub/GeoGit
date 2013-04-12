/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;


public class MergeConflictsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MergeConflictsException(String msg) {
        super(msg);
    }

}
