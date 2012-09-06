/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.plumbing;

import java.io.File;
import java.net.URL;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.command.plumbing.ResolveGeogitDir;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 */
@Parameters(commandNames = "rev-parse", commandDescription = "Resolve parameters according to the arguments")
public class RevParse extends AbstractCommand {

    @Parameter(names = "--resolve-geogit-dir", description = "Check if the current directory is inside a geogit repository and print out the repository location")
    private boolean resolve_geogit_dir;

    @Parameter(names = "--is-inside-work-tree", description = "Check if the current directory is inside a geogit repository and print out the repository location")
    private boolean is_inside_work_tree;

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {

        if (resolve_geogit_dir) {
            resolveGeogitDir(cli);
        } else if (is_inside_work_tree) {
            isInsideWorkTree(cli);
        }

    }

    private void isInsideWorkTree(GeogitCLI cli) throws Exception {
        GeoGIT geoGIT = new GeoGIT(cli.getPlatform().pwd());
        URL repoUrl = geoGIT.command(ResolveGeogitDir.class).call();

        if (null == repoUrl) {
            cli.getConsole().println(
                    "Error: not a geogit repository (or any parent) '"
                            + cli.getPlatform().pwd().getAbsolutePath() + "'");
        } else {
            boolean insideWorkTree = !cli.getPlatform().pwd().getAbsolutePath().contains(".geogit");
            cli.getConsole().println(String.valueOf(insideWorkTree));
        }
    }

    private void resolveGeogitDir(GeogitCLI cli) throws Exception {
        ConsoleReader console = cli.getConsole();

        GeoGIT geoGIT = new GeoGIT(cli.getPlatform().pwd());
        URL repoUrl = geoGIT.command(ResolveGeogitDir.class).call();
        if (null == repoUrl) {
            File currDir = cli.getPlatform().pwd();
            console.println("Error: not a geogit dir '"
                    + currDir.getCanonicalFile().getAbsolutePath() + "'");
        } else if ("file:".equals(repoUrl.getProtocol())) {
            console.println(new File(repoUrl.toURI()).getCanonicalFile().getAbsolutePath());
        } else {
            console.println(repoUrl.toExternalForm());
        }
    }

}
