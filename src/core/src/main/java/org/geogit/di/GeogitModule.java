/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.di;

import org.geogit.api.CommandLocator;
import org.geogit.api.DefaultPlatform;
import org.geogit.api.Platform;
import org.geogit.repository.Index;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.fs.FileObjectDatabase;
import org.geogit.storage.fs.FileRefDatabase;
import org.geogit.storage.fs.IniConfigDatabase;
import org.geogit.storage.hessian.HessianFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 *
 */
public class GeogitModule extends AbstractModule {

    /**
     * 
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        bind(CommandLocator.class).to(GuiceCommandLocator.class).in(Scopes.SINGLETON);

        bind(Platform.class).to(DefaultPlatform.class).asEagerSingleton();

        bind(Repository.class).in(Scopes.SINGLETON);
        bind(ConfigDatabase.class).to(IniConfigDatabase.class).in(Scopes.SINGLETON);
        bind(StagingArea.class).to(Index.class).in(Scopes.SINGLETON);
        bind(WorkingTree.class).in(Scopes.SINGLETON);

        bind(ObjectDatabase.class).to(FileObjectDatabase.class).in(Scopes.SINGLETON);
        bind(RefDatabase.class).to(FileRefDatabase.class).in(Scopes.SINGLETON);

        bind(ObjectSerialisingFactory.class).to(HessianFactory.class).in(Scopes.SINGLETON);
    }
}
