/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.DiffEntry;
import org.geogit.api.GeoGIT;
import org.geogit.api.NothingToCommitException;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 */
@Parameters(commandNames = "commit", commandDescription = "Record staged changes to the repository")
public class Commit extends AbstractCommand implements CLICommand {

    @Parameter(names = "-m", description = "Commit message")
    private String message;

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkState(message != null && !message.trim().isEmpty(), "No commit message provided");

        ConsoleReader console = cli.getConsole();

        final GeoGIT geogit = cli.getGeogit();

        Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());

        // TODO: get committer from config db
        RevCommit commit;
        try {
            commit = geogit.commit().setAuthor("Gabriel Roldan").setMessage(message)
                    .setProgressListener(cli.getProgressListener()).call();
        } catch (NothingToCommitException noChanges) {
            console.println(ansi.fg(Color.RED).a(noChanges.getMessage()).reset().toString());
            return;
        }
        ObjectId parentId = commit.getParentIds().get(0);

        console.println("[" + commit.getId() + "] " + commit.getMessage());

        console.print("Committed, counting objects...");
        Iterator<DiffEntry> diff = geogit.diff().setOldVersion(parentId)
                .setNewVersion(commit.getId()).call();

        int adds = 0, deletes = 0, changes = 0;
        DiffEntry diffEntry;
        while (diff.hasNext()) {
            diffEntry = diff.next();
            switch (diffEntry.getType()) {
            case ADD:
                ++adds;
                break;
            case DELETE:
                ++deletes;
                break;
            case MODIFY:
                ++changes;
                break;
            }
        }

        ansi.fg(Color.GREEN).a(adds).reset().a(" features added, ").fg(Color.YELLOW).a(changes)
                .reset().a(" changed, ").fg(Color.RED).a(deletes).reset().a(" deleted.").reset()
                .newline();

        console.print(ansi.toString());
    }
}
