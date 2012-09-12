/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.di;

import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;

import com.google.inject.AbstractModule;

/**
 *
 */
public class PlumbingCommands extends AbstractModule {

    /**
     * 
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        bind(ResolveGeogitDir.class);
        bind(RevParse.class);
        bind(RefParse.class);
        bind(UpdateRef.class);
        bind(UpdateSymRef.class);
        bind(ResolveObjectType.class);
    }

}
