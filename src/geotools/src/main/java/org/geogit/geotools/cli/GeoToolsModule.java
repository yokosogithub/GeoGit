/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.cli;

import org.geogit.cli.CLIModule;

import com.google.inject.Binder;

/**
 * Provides bindings for GeoTools command extensions to the GeoGit command line interface.
 * 
 * @see PGCommandProxy
 * @see ShpCommandProxy
 */
public class GeoToolsModule implements CLIModule {

    /**
     * @see CLIModule#configure(com.google.inject.Binder)
     */
    @Override
    public void configure(Binder binder) {
        binder.bind(PropertiesEditCommandProxy.class);
        binder.bind(PGCommandProxy.class);
        binder.bind(ShpCommandProxy.class);
    }

}
