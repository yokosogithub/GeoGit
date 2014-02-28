package org.geogit.web.cli;

import org.geogit.cli.CLIModule;
import org.geogit.web.cli.commands.Serve;

import com.google.inject.Binder;

public class ServeModule implements CLIModule {

    @Override
    public void configure(Binder binder) {
        binder.bind(Serve.class);
    }

}
