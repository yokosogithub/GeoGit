/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli;

import org.geogit.cli.porcelain.Add;
import org.geogit.cli.porcelain.Branch;
import org.geogit.cli.porcelain.Checkout;
import org.geogit.cli.porcelain.CherryPick;
import org.geogit.cli.porcelain.Commit;
import org.geogit.cli.porcelain.Help;
import org.geogit.cli.porcelain.Init;
import org.geogit.cli.porcelain.Log;
import org.geogit.cli.porcelain.Status;

import com.google.inject.AbstractModule;

/**
 * Guice module providing builtin commands for the {@link GeogitCLI CLI} app.
 * 
 * @see Add
 * @see Branch
 * @see Checkout
 * @see CherryPick
 * @see Commit
 * @see Help
 * @see Init
 * @see Log
 * @see Status
 */
public class BuiltinCommandsModule extends AbstractModule implements CLIModule {

    @Override
    protected void configure() {
        bind(Add.class);
        bind(Branch.class);
        bind(Checkout.class);
        bind(CherryPick.class);
        bind(Commit.class);
        bind(Help.class);
        bind(Init.class);
        bind(Log.class);
        bind(Status.class);
    }

}
