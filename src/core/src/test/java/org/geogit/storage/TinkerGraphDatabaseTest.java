package org.geogit.storage;

import org.geogit.di.GeogitModule;
import org.geogit.storage.GraphDatabaseTest;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TinkerGraphDatabaseTest extends GraphDatabaseTest {
    protected Injector createInjector() {
        return Guice.createInjector(new GeogitModule()); // relies on TinkerGraph being default
    }
}

