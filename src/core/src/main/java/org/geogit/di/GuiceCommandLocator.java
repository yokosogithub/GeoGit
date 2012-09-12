/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.di;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.CommandLocator;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 *
 */
public class GuiceCommandLocator implements CommandLocator {

    private Injector injector;

    @Inject
    public GuiceCommandLocator(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <T extends AbstractGeoGitOp<?>> T command(Class<T> commandClass) {
        T instance = injector.getInstance(commandClass);
        return instance;
    }
}
