/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.api;

import org.geogit.repository.Index;
import org.geogit.repository.Repository;
import org.geogit.repository.StagingArea;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.bdbje.EnvironmentBuilder;
import org.geogit.storage.bdbje.JEObjectDatabase;
import org.geogit.storage.bdbje.JERefDatabase;
import org.geogit.storage.bdbje.JEStagingDatabase;
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

        bind(Platform.class).to(DefaultPlatform.class).asEagerSingleton();
        bind(Repository.class).in(Scopes.SINGLETON);
        bind(StagingArea.class).to(Index.class).in(Scopes.SINGLETON);

        // JE bindings
        bind(ObjectDatabase.class).to(JEObjectDatabase.class).in(Scopes.SINGLETON);
        bind(StagingDatabase.class).to(JEStagingDatabase.class).in(Scopes.SINGLETON);
        bind(RefDatabase.class).to(JERefDatabase.class).in(Scopes.SINGLETON);

        bind(EnvironmentBuilder.class).in(Scopes.NO_SCOPE);
        bind(ObjectSerialisingFactory.class).to(HessianFactory.class).in(Scopes.SINGLETON);

    }
}
