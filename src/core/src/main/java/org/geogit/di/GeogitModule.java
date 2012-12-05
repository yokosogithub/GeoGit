/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.di;

import static com.google.inject.matcher.Matchers.subclassesOf;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.geogit.api.CommandLocator;
import org.geogit.api.DefaultPlatform;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.repository.Index;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;
import org.geogit.storage.CachingObjectDatabaseGetInterceptor;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.fs.FileObjectDatabase;
import org.geogit.storage.fs.FileRefDatabase;
import org.geogit.storage.fs.IniConfigDatabase;
import org.geogit.storage.hessian.HessianFactory;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matcher;

/**
 * Provides bindings for GeoGit singletons.
 * 
 * @see CommandLocator
 * @see Platform
 * @see Repository
 * @see ConfigDatabase
 * @see StagingArea
 * @see WorkingTree
 * @see ObjectDatabase
 * @see RefDatabase
 * @see ObjectSerialisingFactory
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

        final Method interceptedGettter;
        try {
            interceptedGettter = ObjectDatabase.class.getMethod("get", ObjectId.class, Class.class);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        Matcher<Method> methodMatcher = new Matcher<Method>() {

            @Override
            public boolean matches(Method t) {
                if (interceptedGettter.getName().equals(t.getName())) {
                    if (Arrays
                            .equals(interceptedGettter.getParameterTypes(), t.getParameterTypes())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Matcher<Method> and(Matcher<? super Method> other) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Matcher<Method> or(Matcher<? super Method> other) {
                throw new UnsupportedOperationException();
            }
        };

        bindInterceptor(subclassesOf(ObjectDatabase.class), methodMatcher,
                new CachingObjectDatabaseGetInterceptor());
    }
}
