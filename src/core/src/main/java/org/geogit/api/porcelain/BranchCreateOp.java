/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;

public class BranchCreateOp extends AbstractGeoGitOp<Ref> {

    private String branchName;

    public BranchCreateOp() {
    }

    public BranchCreateOp setName(final String branchName) {
        this.branchName = branchName;
        return this;
    }

    public Ref call() throws Exception {
        throw new UnsupportedOperationException();
    }

}
