/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Incorporates changes from a remote repository into the current branch.
 * <p>
 * More precisely, {@code geogit pull} runs {@code geogit fetch} with the given parameters and calls
 * {@code geogit merge} to merge the retrieved branch heads into the current branch. With
 * {@code --rebase}, it runs {@code geogit rebase} instead of {@code geogit merge}.
 * <p>
 * CLI proxy for {@link PullOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit pull [options] [<repository> [<refspec>...]]}
 * </ul>
 * 
 * @see PullOp
 */
@Parameters(commandNames = "pull", commandDescription = "Fetch from and merge with another repository or a local branch")
public class Pull extends AbstractCommand implements CLICommand {

    @Parameter(names = "--all", description = "Fetch all remotes.")
    private boolean all = false;

    @Parameter(names = "--rebase", description = "Rebase the current branch on top of the upstream branch after fetching.")
    private boolean rebase = false;

    @Parameter(description = "[<repository> [<refspec>...]]")
    private List<String> args;

    /**
     * Executes the pull command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        if (!rebase) {
            throw new UnsupportedOperationException(
                    "Merge pull not yet implemented, use --rebase instead.");
        }

    }
}
