/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;

public class CheckoutOp extends AbstractGeoGitOp<Ref> {

    private String refName;

    public CheckoutOp() {
    }

    public CheckoutOp setName(final String refName) {
        this.refName = refName;
        return this;
    }

    public Ref call() throws Exception {
        throw new UnsupportedOperationException("not yet implemented");
    }

}
