/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage.bdbje;

import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 *
 */
public class JEStorageModule extends AbstractModule {

    @Override
    protected void configure() {
        // JE bindings
        bind(ObjectDatabase.class).to(JEObjectDatabase.class).in(Scopes.SINGLETON);
        bind(StagingDatabase.class).to(JEStagingDatabase.class).in(Scopes.SINGLETON);

        bind(EnvironmentBuilder.class).in(Scopes.NO_SCOPE);
    }

}
