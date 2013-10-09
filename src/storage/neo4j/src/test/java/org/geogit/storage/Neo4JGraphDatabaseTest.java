package org.geogit.storage;

import org.geogit.di.GeogitModule;
import org.geogit.storage.GraphDatabaseTest;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;


public class Neo4JGraphDatabaseTest extends GraphDatabaseTest {
    protected Injector createInjector() {
        return Guice.createInjector(
            Modules.override(new GeogitModule()).with(new Neo4JTestModule()));
    }
}
