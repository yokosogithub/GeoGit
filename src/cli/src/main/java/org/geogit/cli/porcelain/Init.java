/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.geogit.api.GeoGIT;
import org.geogit.api.InitOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.command.plumbing.ResolveGeogitDir;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Throwables;

/**
 *
 */
@Parameters(commandNames = "init", commandDescription = "Create an empty geogit repository or reinitialize an existing one")
public class Init extends AbstractCommand implements CLICommand {

    @Parameter(names = "location", description = "Repository location (directory).", required = false)
    private URI location;

    @Override
    public void runInternal(GeogitCLI cli/* TODO , ProgressListener progress */) {

        try {
            GeoGIT geogit = new GeoGIT();
            geogit.command(InitOp.class).call();
            cli.setGeogit(geogit);

            URL envHome = geogit.command(ResolveGeogitDir.class).call();

            cli.getConsole().println(
                    "Repository created at " + new File(envHome.toURI()).getAbsolutePath());

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

}