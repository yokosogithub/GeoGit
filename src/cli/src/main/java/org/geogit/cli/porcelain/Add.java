/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.porcelain.AddOp;
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

    @Parameter(description = "<patterns>...")
    private List<String> patterns = new ArrayList<String>();

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        ConsoleReader console = cli.getConsole();

        String pathFilter = null;
        if (patterns.size() == 1) {
            pathFilter = patterns.get(0);
        } else if (patterns.size() > 1) {
            throw new UnsupportedOperationException("Only a single path is supported so far");
        }

        console.print("Counting unstaged features...");
        int unstaged = cli.getGeogit().getRepository().getIndex().getDatabase()
                .countUnstaged(pathFilter);
        if (0 == unstaged) {
            console.println();
            console.println("No unstaged features, exiting.");
            return;
        } else {
            console.println(String.valueOf(unstaged));
        }

        console.println("Staging changes...");
        AddOp op = cli.getGeogit().add();
        if (patterns.size() == 1) {
            op.addPattern(patterns.get(0));
        } else if (patterns.size() > 1) {
            throw new UnsupportedOperationException("Only a single path is supported so far");
        }
        StagingArea index = op.setProgressListener(cli.getProgressListener()).call();

        int staged = index.getDatabase().countStaged(null);
        unstaged = index.getDatabase().countUnstaged(null);

        console.println(staged + " features staged for commit");
        console.println(unstaged + " features not staged for commit");
    }

}
