/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

/**
 * Exception that indicates that a revert operation cannot be finished due to merge conflicts
 */
public class RevertConflictsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RevertConflictsException(String msg) {
        super(msg);
    }

}
