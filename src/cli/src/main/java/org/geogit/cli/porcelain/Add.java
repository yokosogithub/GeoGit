/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.diff.DiffObjectCount;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;
import org.geogit.api.porcelain.AddOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.InvalidParameterException;
import org.geogit.cli.annotation.ObjectDatabaseReadOnly;
import org.geogit.repository.WorkingTree;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * This command updates the index using the current content found in the working tree, to prepare
 * the content staged for the next commit. It typically adds all unstaged changes, but with a
 * defined pattern, only matching features will be added.
 * <p>
 * The "index" holds a snapshot of the HEAD tree plus any staged changes and is used to determine
 * what will be committed to the repository. Thus after making any changes to the working tree, and
 * before running the commit command, you must use the add command to add any new or modified files
 * to the index.
 * <p>
 * This command can be performed multiple times before a commit. It only adds the content of the
 * specified feature(s) at the time the add command is run; if you want subsequent changes included
 * in the next commit, then you must run {@code geogit add} again to add the new content to the
 * index.
 * <p>
 * CLI proxy for {@link AddOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit add [-n] [<pattern>...]}
 * </ul>
 * 
 * @see AddOp
 */
@ObjectDatabaseReadOnly
@Parameters(commandNames = "add", commandDescription = "Add features to the staging area")
public class Add extends AbstractCommand implements CLICommand {

    @Parameter(names = { "--dry-run", "-n" }, description = "Maximum number of commits to log")
    private boolean dryRun;

    @Parameter(names = { "--update", "-u" }, description = "Only add features that have already been tracked")
    private boolean updateOnly;

    @Parameter(description = "<patterns>...")
    private List<String> patterns = new ArrayList<String>();

    /**
     * Executes the add command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws IOException {
        final GeoGIT geogit = cli.getGeogit();

        final ConsoleReader console = cli.getConsole();

        String pathFilter = null;
        if (patterns.size() == 1) {
            pathFilter = patterns.get(0);
        } else if (patterns.size() > 1) {
            throw new InvalidParameterException("Only a single path is supported so far");
        }

        List<Conflict> conflicts = geogit.command(ConflictsReadOp.class).call();

        console.print("Counting unstaged elements...");
        DiffObjectCount unstaged = geogit.getRepository().getWorkingTree()
                .countUnstaged(pathFilter);
        if (0 == unstaged.getCount() && conflicts.isEmpty()) {
            console.println();
            console.println("No unstaged elements, exiting.");
            return;
        } else {
            console.println(String.valueOf(unstaged.getCount()));
        }

        console.println("Staging changes...");
        AddOp op = geogit.command(AddOp.class);
        if (patterns.size() == 1) {
            op.addPattern(patterns.get(0));
        }

        WorkingTree workTree = op.setUpdateOnly(updateOnly)
                .setProgressListener(cli.getProgressListener()).call();

        DiffObjectCount staged = geogit.getRepository().getIndex().countStaged(null);
        unstaged = workTree.countUnstaged(null);

        console.println(staged.getFeaturesCount() + " features and " + staged.getTreesCount()
                + " trees staged for commit");
        console.println(unstaged.getFeaturesCount() + " features and " + unstaged.getTreesCount()
                + " trees not staged for commit");
    }

}
