/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;
import jline.console.ConsoleReader;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.StagingArea;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 */
@Parameters(commandNames = "add", commandDescription = "Add features to the staging area")
public class Add extends AbstractCommand implements CLICommand {

    @Parameter(names = { "--dry-run", "-n" }, description = "Maximum number of commits to log")
    private boolean dryRun;

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        ConsoleReader console = cli.getConsole();

        console.println("Counting unstaged features...");
        int unstaged = cli.getGeogit().getRepository().getIndex().getDatabase().countUnstaged(null);
        if (0 == unstaged) {
            console.println("No unstaged features, exiting.");
            return;
        }

        console.println("Staging changes...");
        StagingArea index = cli.getGeogit().add().setProgressListener(cli.getProgressListener())
                .call();

        int staged = index.getDatabase().countStaged(null);
        unstaged = index.getDatabase().countUnstaged(null);

        console.println(staged + " features staged for commit");
        console.println(unstaged + " features not staged for commit");
    }

}
