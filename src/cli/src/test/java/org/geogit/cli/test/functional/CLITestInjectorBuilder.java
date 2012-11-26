package org.geogit.cli.test.functional;

import org.geogit.api.InjectorBuilder;
import org.geogit.api.Platform;
import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class CLITestInjectorBuilder extends InjectorBuilder {

    Platform platform;

    public CLITestInjectorBuilder(Platform platform) {
        this.platform = platform;
    }

    @Override
    public Injector get() {
        return Guice.createInjector(Modules.override(new GeogitModule()).with(
                new JEStorageModule(), new TestModule(platform)));
    }

}
