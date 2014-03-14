/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.List;

import org.geogit.api.porcelain.PushOp;
import org.geogit.api.porcelain.SynchronizationException;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Updates remote refs using local refs, while sending objects necessary to complete the given refs.
 * <p>
 * CLI proxy for {@link PushOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit push [<options>] [<repository> [<refspec>...]]}
 * </ul>
 * 
 * @see PushOp
 */
@ReadOnly
@Parameters(commandNames = "push", commandDescription = "Update remote refs along with associated objects")
public class Push extends AbstractCommand implements CLICommand {

    @Parameter(names = "--all", description = "Instead of naming each ref to push, specifies that all refs under refs/heads/ be pushed.")
    private boolean all = false;

    @Parameter(description = "[<repository> [<refspec>...]]")
    private List<String> args;

    /**
     * Executes the push command using the provided options.
     */
    @Override
    public void runInternal(GeogitCLI cli) {

        PushOp push = cli.getGeogit().command(PushOp.class);
        push.setProgressListener(cli.getProgressListener());
        push.setAll(all);

        if (args != null) {
            if (args.size() > 0) {
                push.setRemote(args.get(0));
            }
            for (int i = 1; i < args.size(); i++) {
                push.addRefSpec(args.get(i));
            }
        }
        try {
            // TODO: listen on progress?
            push.call();
        } catch (SynchronizationException e) {
            switch (e.statusCode) {
            case NOTHING_TO_PUSH:
                throw new CommandFailedException("Nothing to push.", e);
            case REMOTE_HAS_CHANGES:
                throw new CommandFailedException(
                        "Push failed: The remote repository has changes that would be lost in the event of a push.",
                        e);
            case HISTORY_TOO_SHALLOW:
                throw new CommandFailedException(
                        "Push failed: There is not enough local history to complete the push.", e);
            }
        }
    }
}
