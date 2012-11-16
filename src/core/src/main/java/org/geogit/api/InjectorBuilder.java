package org.geogit.api;

import org.geogit.di.GeogitModule;

import com.google.common.base.Supplier;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class InjectorBuilder implements Supplier<Injector> {

    @Override
    public Injector get() {
        return Guice.createInjector(new GeogitModule());
    }

}
