/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.spatial;

import org.geogit.cli.CLIModule;

import com.google.inject.AbstractModule;

/**
 * Guice module providing import-export commands for different spatial formats.
 * 
 * @see ImportShp
 */
public class SpatialModule extends AbstractModule implements CLIModule {

    @Override
    protected void configure() {
        bind(ImportShp.class);
    }

}
