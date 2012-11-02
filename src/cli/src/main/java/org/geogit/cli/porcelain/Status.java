/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import java.io.IOException;
import java.util.Iterator;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.StagingArea;
import org.geogit.repository.WorkingTree;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Displays features that have differences between the index and the current HEAD commit and
 * features that have differences between the working tree and the index file. The first are what
 * you would commit by running geogit commit; the second are what you could commit by running geogit
 * add before running geogit commit.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit status [<options>]}
 * </ul>
 * 
 * @see Commit
 * @see Add
 */
@Parameters(commandNames = "status", commandDescription = "Show the working tree status")
public class Status implements CLICommand {

    @Parameter(names = "--color", description = "Whether to apply colored output. Possible values are auto|never|always.", converter = ColorArg.Converter.class)
    private ColorArg coloredOutput = ColorArg.auto;

    @Parameter(names = "--limit", description = "Limit number of displayed changes. Must be >= 0.")
    private Integer limit = 50;

    @Parameter(names = "--all", description = "Force listing all changes (overrides limit).")
    private boolean all = false;

    /**
     * Executes the status command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        if (limit < 0) {
            cli.getConsole().println("Limit must be 0 or greater.");
            return;
        }

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        final StagingArea index = geogit.getRepository().getIndex();

        final WorkingTree workTree = geogit.getRepository().getWorkingTree();

        String pathFilter = null;
        final int countStaged = index.countStaged(pathFilter);
        final int countUnstaged = workTree.countUnstaged(pathFilter);

        console.println("# On branch <can't know yet>");

        if (countStaged == 0 && countUnstaged == 0) {
            console.println("nothing to commit (working directory clean)");
            return;
        }

        if (countStaged > 0) {
            Iterator<DiffEntry> staged = geogit.command(DiffIndex.class).setFilter(pathFilter)
                    .call();

            console.println("# Changes to be committed:");
            console.println("#   (use \"geogit reset HEAD <path/to/fid>...\" to unstage)");

            console.println("#");

            print(console, staged, Color.GREEN, countStaged);

            console.println("#");
        }

        if (countUnstaged > 0) {
            Iterator<DiffEntry> unstaged = geogit.command(DiffWorkTree.class).setFilter(pathFilter)
                    .call();
            console.println("# Changes not staged for commit:");
            console.println("#   (use \"geogit add <path/to/fid>...\" to update what will be committed");
            console.println("#   (use \"geogit checkout -- <path/to/fid>...\" to discard changes in working directory");
            console.println("#");
            print(console, unstaged, Color.RED, countUnstaged);
        }
    }

    /**
     * Prints the list of changes using the specified options
     * 
     * @param console the output console
     * @param changes an iterator of differences to print
     * @param color the color to use for the changes if color use is enabled
     * @param total the total number of changes
     * @throws IOException
     * @see DiffEntry
     */
    private void print(final ConsoleReader console, final Iterator<DiffEntry> changes,
            final Color color, final int total) throws IOException {

        final int limit = all || this.limit == null ? Integer.MAX_VALUE : this.limit.intValue();

        StringBuilder sb = new StringBuilder();

        boolean useColor;
        switch (this.coloredOutput) {
        case never:
            useColor = false;
            break;
        case always:
            useColor = true;
            break;
        default:
            useColor = console.getTerminal().isAnsiSupported();
        }

        Ansi ansi = AnsiDecorator.newAnsi(useColor, sb);

        DiffEntry entry;
        ChangeType type;
        String path;
        int cnt = 0;
        while (changes.hasNext() && cnt < limit) {
            ++cnt;

            entry = changes.next();
            type = entry.changeType();
            path = formatPath(entry);

            sb.setLength(0);
            ansi.a("#      ").fg(color).a(type.toString().toLowerCase()).a("  ").a(path).reset();
            console.println(ansi.toString());
        }

        sb.setLength(0);
        ansi.a("# ").a(total).reset().a(" total.");
        console.println(ansi.toString());
    }

    /**
     * Formats a DiffEntry for display
     * 
     * @param entry the DiffEntry to format
     * @return the formatted display string
     * @see DiffEntry
     */
    private String formatPath(DiffEntry entry) {
        String path;
        NodeRef oldObject = entry.getOldObject();
        NodeRef newObject = entry.getNewObject();
        if (oldObject == null) {
            path = newObject.getPath();
        } else if (newObject == null) {
            path = oldObject.getPath();
        } else {
            if (oldObject.getPath().equals(newObject.getPath())) {
                path = oldObject.getPath();
            } else {
                path = oldObject.getPath() + " -> " + newObject.getPath();
            }
        }
        return path;
    }
}
