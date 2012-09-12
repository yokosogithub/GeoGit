/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import org.geogit.api.AbstractGeoGitOp;

public class BranchDeleteOp extends AbstractGeoGitOp<String> {

    private String branchName;

    private boolean force;

    public BranchDeleteOp() {
    }

    /**
     * @return the name of the branch deleted
     * @see java.util.concurrent.Callable#call()
     */
    public String call() throws Exception {
        throw new UnsupportedOperationException("not yet implemented");
    }

    public BranchDeleteOp setName(final String branchName) {
        this.branchName = branchName;
        return this;
    }

    public BranchDeleteOp setForce(final boolean force) {
        this.force = force;
        return this;
    }

}
