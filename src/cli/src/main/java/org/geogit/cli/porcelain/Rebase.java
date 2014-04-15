/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.IOException;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.CheckoutException;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.RebaseConflictsException;
import org.geogit.api.porcelain.RebaseOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

/**
 * Forward-port local commits to the updated upstream head.
 * <p>
 * If {@code <branch>} is specified, {@code geogit rebase} will perform an automatic
 * {@code geogit checkout <branch>} before doing anything else. Otherwise it remains on the current
 * branch.
 * <p>
 * All changes made by commits in the current branch but that are not in {@code <upstream>} are
 * saved to a temporary area.
 * <p>
 * The current branch is reset to {@code <upstream>}, or {@code <newbase>} if the {@code --onto}
 * option was supplied.
 * <p>
 * The commits that were previously saved into the temporary area are then reapplied to the current
 * branch, one by one, in order.
 * <p>
 * CLI proxy for {@link RebaseOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit rebase [--onto <newbase>] [<upstream>] [<branch>]}
 * </ul>
 * 
 * @see RebaseOp
 */
@Parameters(commandNames = { "rebase" }, commandDescription = "Forward-port local commits to the updated upstream head")
public class Rebase extends AbstractCommand implements CLICommand {

    @Parameter(names = { "--onto" }, description = "Starting point at which to create the new commits.")
    private String onto;

    @Parameter(names = { "--abort" }, description = "Abort a conflicted rebase.")
    private boolean abort;

    @Parameter(names = { "--continue" }, description = "Continue a conflicted rebase.")
    private boolean continueRebase;

    @Parameter(names = { "--skip" }, description = "Skip the current conflicting commit.")
    private boolean skip;

    @Parameter(names = { "--squash" }, description = "Squash commits instead of applying them one by one. A message has to be provided to use for the squashed commit")
    private String squash;

    @Parameter(description = "[<upstream>] [<branch>]")
    private List<String> arguments;

    /**
     * Executes the rebase command using the provided options.
     */
    @Override
    public void runInternal(GeogitCLI cli) throws IOException {

        checkParameter(!(skip && continueRebase), "Cannot use both --skip and --continue");
        checkParameter(!(skip && abort), "Cannot use both --skip and --abort");
        checkParameter(!(abort && continueRebase), "Cannot use both --abort and --continue");

        GeoGIT geogit = cli.getGeogit();
        RebaseOp rebase = geogit.command(RebaseOp.class).setSkip(skip).setContinue(continueRebase)
                .setAbort(abort).setSquashMessage(squash);
        rebase.setProgressListener(cli.getProgressListener());

        if (arguments == null || arguments.size() == 0) {
            if (abort || skip || continueRebase) {
            } else {
                // Rebase onto remote branch
                throw new UnsupportedOperationException("remote branch rebase not yet supported");
            }
        } else {
            checkParameter(arguments.size() < 3, "Too many arguments specified.");
            if (arguments.size() == 2) {
                // Make sure branch is valid
                Optional<ObjectId> branchRef = geogit.command(RevParse.class)
                        .setRefSpec(arguments.get(1)).call();
                checkParameter(branchRef.isPresent(), "The branch reference could not be resolved.");
                // Checkout <branch> prior to rebase
                try {
                    geogit.command(CheckoutOp.class).setSource(arguments.get(1)).call();
                } catch (CheckoutException e) {
                    throw new CommandFailedException(e.getMessage(), e);
                }

            }

            Optional<Ref> upstreamRef = geogit.command(RefParse.class).setName(arguments.get(0))
                    .call();
            checkParameter(upstreamRef.isPresent(), "The upstream reference could not be resolved.");
            rebase.setUpstream(Suppliers.ofInstance(upstreamRef.get().getObjectId()));
        }

        if (onto != null) {
            Optional<ObjectId> ontoId = geogit.command(RevParse.class).setRefSpec(onto).call();
            checkParameter(ontoId.isPresent(), "The onto reference could not be resolved.");
            rebase.setOnto(Suppliers.ofInstance(ontoId.get()));
        }

        try {
            rebase.call();
        } catch (RebaseConflictsException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage() + "\n");
            sb.append("When you have fixed this conflicts, run 'geogit rebase --continue' to continue rebasing.\n");
            sb.append("If you would prefer to skip this commit, instead run 'geogit rebase --skip.\n");
            sb.append("To check out the original branch and stop rebasing, run 'geogit rebase --abort'\n");
            throw new CommandFailedException(sb.toString());
        }

        if (abort) {
            cli.getConsole().println("Rebase aborted successfully.");
        }
    }
}
