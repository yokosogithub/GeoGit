/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.plumbing.ParseTimestamp;
import org.geogit.api.plumbing.ResolveObjectType;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.merge.ReadMergeCommitMessageOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Stores the current contents of the index in a new commit along with a log message from the user
 * describing the changes.
 * <p>
 * CLI proxy for {@link CommitOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit commit -m <msg>}
 * </ul>
 * 
 * @see CommitOp
 */
@Parameters(commandNames = "commit", commandDescription = "Record staged changes to the repository")
public class Commit extends AbstractCommand implements CLICommand {

    @Parameter(names = "-m", description = "Commit message")
    private String message;

    @Parameter(names = "-c", description = "Commit to reuse")
    private String commitToReuse;

    @Parameter(names = "-t", description = "Commit timestamp")
    private String commitTimestamp;

    @Parameter(names = "--amend", description = "Amends last commit")
    private boolean amend;

    @Parameter(description = "<pathFilter>  [<paths_to_commit]...")
    private List<String> pathFilters = Lists.newLinkedList();

    /**
     * Executes the commit command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        final GeoGIT geogit = cli.getGeogit();

        if (message == null || Strings.isNullOrEmpty(message)) {
            message = geogit.command(ReadMergeCommitMessageOp.class).call();
        }
        checkState(!Strings.isNullOrEmpty(message) || commitToReuse != null || amend,
                "No commit message provided");

        ConsoleReader console = cli.getConsole();

        Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());

        RevCommit commit;
        try {
            CommitOp commitOp = geogit.command(CommitOp.class).setMessage(message).setAmend(amend);
            if (commitTimestamp != null && !Strings.isNullOrEmpty(commitTimestamp)) {
                Long millis = geogit.command(ParseTimestamp.class).setString(commitTimestamp)
                        .call();
                commitOp.setCommitterTimestamp(millis.longValue());
            }

            if (commitToReuse != null) {
                Optional<ObjectId> commitId = geogit.command(RevParse.class)
                        .setRefSpec(commitToReuse).call();
                checkState(commitId.isPresent(), "Provided reference does not exist");
                TYPE type = geogit.command(ResolveObjectType.class).setObjectId(commitId.get())
                        .call();
                checkState(TYPE.COMMIT.equals(type),
                        "Provided reference does not resolve to a commit");
                commitOp.setCommit(geogit.getRepository().getCommit(commitId.get()));
            }
            commit = commitOp.setPathFilters(pathFilters)
                    .setProgressListener(cli.getProgressListener()).call();
        } catch (NothingToCommitException noChanges) {
            console.println(ansi.fg(Color.RED).a(noChanges.getMessage()).reset().toString());
            throw new CommandFailedException();
        }
        final ObjectId parentId = commit.parentN(0).or(ObjectId.NULL);

        console.println("[" + commit.getId() + "] " + commit.getMessage());

        console.print("Committed, counting objects...");
        Iterator<DiffEntry> diff = geogit.command(DiffOp.class).setOldVersion(parentId)
                .setNewVersion(commit.getId()).call();

        int adds = 0, deletes = 0, changes = 0;
        DiffEntry diffEntry;
        while (diff.hasNext()) {
            diffEntry = diff.next();
            switch (diffEntry.changeType()) {
            case ADDED:
                ++adds;
                break;
            case REMOVED:
                ++deletes;
                break;
            case MODIFIED:
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
