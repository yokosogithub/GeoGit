package org.geogit.cli;

import org.geogit.api.InjectorBuilder;
import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class CLIInjectorBuilder extends InjectorBuilder {

    @Override
    public Injector get() {
        return Guice.createInjector(Modules.override(new GeogitModule())
                .with(new JEStorageModule()));
    }

}
