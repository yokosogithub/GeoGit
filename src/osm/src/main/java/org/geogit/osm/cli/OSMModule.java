/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli;

import org.geogit.cli.CLIModule;

import com.google.inject.Binder;

/**
 *
 */
public class OSMModule implements CLIModule {

    @Override
    public void configure(Binder binder) {
        binder.bind(OSMCommandProxy.class);
    }

}
