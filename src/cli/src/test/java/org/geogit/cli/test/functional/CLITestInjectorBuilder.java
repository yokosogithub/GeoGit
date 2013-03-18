package org.geogit.cli.test.functional;

import java.io.File;

import org.geogit.api.InjectorBuilder;
import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class CLITestInjectorBuilder extends InjectorBuilder {

    File workingDirectory;

    File homeDirectory;

    public CLITestInjectorBuilder(File workingDirectory, File homeDirectory) {
        this.workingDirectory = workingDirectory;
        this.homeDirectory = homeDirectory;
    }

    @Override
    public Injector build() {
        return Guice.createInjector(Modules.override(new GeogitModule()).with(
                new JEStorageModule(),
                new FunctionalTestModule(new TestPlatform(workingDirectory, homeDirectory))));
    }

    @Override
    public Injector buildWithOverrides(Module... overrides) {
        TestPlatform testPlatform = new TestPlatform(workingDirectory, homeDirectory);
        JEStorageModule jeStorageModule = new JEStorageModule();
        FunctionalTestModule functionalTestModule = new FunctionalTestModule(testPlatform);

        return Guice.createInjector(Modules.override(
                Modules.override(new GeogitModule()).with(jeStorageModule, functionalTestModule))
                .with(overrides));
    }
}
