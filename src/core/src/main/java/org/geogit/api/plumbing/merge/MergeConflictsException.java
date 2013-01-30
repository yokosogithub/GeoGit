/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;

import org.geogit.api.porcelain.GeoGitOpException;

public class MergeConflictsException extends GeoGitOpException {

    private static final long serialVersionUID = 1L;

    public MergeConflictsException(String msg) {
        super(msg);
    }

}
