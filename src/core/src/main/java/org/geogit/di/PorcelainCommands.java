/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.di;

import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.InitOp;

import com.google.inject.AbstractModule;

/**
 *
 */
public class PorcelainCommands extends AbstractModule {

    /**
     * 
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        bind(InitOp.class);
        bind(CommitOp.class);
    }

}
