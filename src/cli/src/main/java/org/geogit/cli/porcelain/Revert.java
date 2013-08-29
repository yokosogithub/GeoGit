package org.geogit.cli.porcelain;

import java.io.IOException;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.RevertConflictsException;
import org.geogit.api.porcelain.RevertOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

/**
 * Given one or more existing commits, revert the changes that the related patches introduce, and
 * record some new commits that record them. This requires your working tree to be clean (no
 * modifications from the HEAD commit).
 * 
 * <p>
 * CLI Proxy for {@link RevertOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit revert [--continue] [--abort] [--no-commit] <commit>...}
 * </ul>
 * 
 * @see RevertOp
 */
@Parameters(commandNames = "revert", commandDescription = "Revert commits to undo the changes made")
public class Revert extends AbstractCommand implements CLICommand {

    @Parameter(description = "<commits>...", variableArity = true)
    private List<String> commits = Lists.newArrayList();

    @Parameter(names = "--no-commit", description = "Do not create new commit with reverted changes")
    private boolean noCommit;

    @Parameter(names = "--continue", description = "Continue a revert process stopped because of conflicts")
    private boolean continueRevert;

    @Parameter(names = "--abort", description = "Abort a revert process stopped because of conflicts")
    private boolean abort;

    /**
     * Executes the revert command.
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        checkParameter(commits.size() > 0 || abort || continueRevert,
                "nothing specified for reverting");

        final GeoGIT geogit = cli.getGeogit();
        RevertOp revert = geogit.command(RevertOp.class);

        for (String st : commits) {
            Optional<ObjectId> commitId = geogit.command(RevParse.class).setRefSpec(st).call();
            checkParameter(commitId.isPresent(), "Couldn't resolve '" + st
                    + "' to a commit, aborting revert.");
            revert.addCommit(Suppliers.ofInstance(commitId.get()));
        }
        try {
            revert.setCreateCommit(!noCommit).setAbort(abort).setContinue(continueRevert).call();
        } catch (RevertConflictsException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage() + "\n");
            sb.append("When you have fixed these conflicts, run 'geogit revert --continue' to continue the revert operation.\n");
            sb.append("To abort the revert operation, run 'geogit revert --abort'\n");
            throw new CommandFailedException(sb.toString());
        }

        if (abort) {
            cli.getConsole().println("Revert aborted successfully.");
        }

    }
}
