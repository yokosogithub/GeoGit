/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.di.GeogitModule;
import org.junit.Assert;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TinkerGraphDatabaseStressTest extends GraphDatabaseStressTest {

    @Override
    protected Injector createInjector() {
        Injector injector = Guice.createInjector(new GeogitModule()); // relies on TinkerGraph being
                                                                      // default
        Assert.assertTrue(injector.getInstance(GraphDatabase.class) instanceof TinkerGraphDatabase);
        return injector;
    }
}
