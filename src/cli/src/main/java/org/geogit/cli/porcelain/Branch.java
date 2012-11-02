/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.List;

import jline.Terminal;
import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.BranchDeleteOp;
import org.geogit.api.porcelain.BranchListOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 *
 */
@Parameters(commandNames = { "branch", "br" }, commandDescription = "List, create, or delete branches")
public class Branch extends AbstractCommand implements CLICommand {

    @Parameter(description = "<branch name> [<start point>]")
    private List<String> branchName = Lists.newArrayList();

    @Parameter(names = "--color", description = "Whether to apply colored output. Possible values are auto|never|always.", converter = ColorArg.Converter.class)
    private ColorArg color = ColorArg.auto;

    @Parameter(names = { "--checkout", "-c" }, description = "automatically checkout the new branch when the command is used to create a branch")
    private boolean checkout;

    @Parameter(names = { "--delete", "-D" })
    private boolean delete = false;

    @Parameter(names = { "--verbose", "-v",
            "Verbose output for list mode. Shows branch commit id and commit message." })
    private boolean verbose = false;

    @Parameter(names = { "--remote", "-r" }, description = "List or delete (if used with -d) the remote-tracking branches.")
    private boolean remotes = false;

    @Parameter(names = { "--all", "-a" }, description = "List all branches, both local and remote")
    private boolean all = false;

    @Override
    public void runInternal(final GeogitCLI cli) {
        final GeoGIT geogit = cli.getGeogit();
        checkState(geogit != null, "not in a geogit repository.");

        final ConsoleReader console = cli.getConsole();

        if (branchName.isEmpty()) {
            listBranches(cli);
            return;
        }

        if (delete) {
            for (String br : branchName) {
                geogit.command(BranchDeleteOp.class).setName(br).call();
                try {
                    console.println("deleted branch " + br);
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }
            return;
        }

        checkArgument(branchName.size() < 3, "too many arguments: " + branchName.toString());

        final String branch = branchName.get(0);
        final String origin = branchName.size() > 1 ? branchName.get(1) : Ref.HEAD;

        Ref newBranch = geogit.command(BranchCreateOp.class).setName(branch)
                .setAutoCheckout(checkout).setSource(origin).call();

        try {
            console.println("Created branch " + newBranch.getName());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void listBranches(GeogitCLI cli) {
        final ConsoleReader console = cli.getConsole();
        final GeoGIT geogit = cli.getGeogit();

        boolean local = all || remotes == false;
        boolean remote = all || remotes;

        ImmutableList<Ref> branches = geogit.command(BranchListOp.class).setLocal(local)
                .setRemotes(remote).call();

        final Terminal terminal = console.getTerminal();
        boolean useColor;
        switch (color) {
        case never:
            useColor = false;
            break;
        case always:
            useColor = true;
            break;
        default:
            useColor = terminal.isAnsiSupported();
        }

        final Ref currentHead = geogit.command(RefParse.class).setName(Ref.HEAD).call().get();

        final int largest = verbose ? largestLenght(branches) : 0;

        for (Ref branchRef : branches) {
            final String branchRefName = branchRef.getName();

            Ansi ansi = AnsiDecorator.newAnsi(useColor);

            if ((currentHead instanceof SymRef)
                    && ((SymRef) currentHead).getTarget().equals(branchRefName)) {
                ansi.a("* ").fg(Color.GREEN);
            } else {
                ansi.a("  ");
            }
            // print unqualified names for local branches
            final String branchName = branchRefName.startsWith(Ref.HEADS_PREFIX) ? branchRefName
                    .substring(Ref.HEADS_PREFIX.length()) : branchRefName;
            ansi.a(branchName);
            ansi.reset();

            if (verbose) {
                ansi.a(Strings.repeat(" ", 1 + (largest - branchName.length())));
                ansi.a(branchRef.getObjectId().toString().substring(0, 7)).a(" ");

                Optional<RevCommit> commit = findCommit(geogit, branchRef);
                if (commit.isPresent()) {
                    ansi.a(messageTitle(commit.get()));
                }
            }

            try {
                console.println(ansi.toString());
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private String messageTitle(RevCommit commit) {
        String message = Optional.fromNullable(commit.getMessage()).or("");
        int newline = message.indexOf('\n');
        return newline == -1 ? message : message.substring(0, newline);
    }

    /**
     * @param branchRef
     * @return
     */
    private Optional<RevCommit> findCommit(GeoGIT geogit, Ref branchRef) {
        ObjectId commitId = branchRef.getObjectId();
        if (commitId.isNull()) {
            return Optional.absent();
        }
        RevCommit commit = geogit.getRepository().getCommit(commitId);
        return Optional.of(commit);
    }

    /**
     * @param branches
     * @return
     */
    private int largestLenght(ImmutableList<Ref> branches) {
        int len = 0;
        for (Ref ref : branches) {
            len = Math.max(len, ref.getName().length());
        }
        return len;
    }
}
