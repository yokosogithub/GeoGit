package org.geogit.api;

import org.geogit.di.GeogitModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class InjectorBuilder {

    public Injector build() {
        return Guice.createInjector(new GeogitModule());
    }

    public Injector buildWithOverrides(Module... overrides) {
        return Guice.createInjector(Modules.override(new GeogitModule()).with(overrides));
    }
}
