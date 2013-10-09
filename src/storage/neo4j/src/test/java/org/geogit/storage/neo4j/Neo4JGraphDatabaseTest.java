/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.neo4j;

import org.geogit.di.GeogitModule;
import org.geogit.storage.GraphDatabaseTest;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class Neo4JGraphDatabaseTest extends GraphDatabaseTest {

    @Override
    protected Injector createInjector() {
        return Guice.createInjector(Modules.override(new GeogitModule())
                .with(new Neo4JTestModule()));
    }
}
