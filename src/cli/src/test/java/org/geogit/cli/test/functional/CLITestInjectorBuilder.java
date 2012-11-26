package org.geogit.cli.test.functional;

import java.io.File;

import org.geogit.api.InjectorBuilder;
import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class CLITestInjectorBuilder extends InjectorBuilder {

    File workingDirectory;

    File homeDirectory;

    public CLITestInjectorBuilder(File workingDirectory, File homeDirectory) {
        this.workingDirectory = workingDirectory;
        this.homeDirectory = homeDirectory;
    }

    @Override
    public Injector get() {
        return Guice.createInjector(Modules.override(new GeogitModule()).with(
                new JEStorageModule(),
                new TestModule(new TestPlatform(workingDirectory, homeDirectory))));
    }

}
