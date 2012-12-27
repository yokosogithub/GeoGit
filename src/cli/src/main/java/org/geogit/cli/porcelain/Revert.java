package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.RevertOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
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
 * <b>NOTE:</b> so far we don't have the ability to merge non conflicting changes. Instead, the diff
 * list we get acts on whole objects, so this operation will not revert feature changes if that
 * feature has been modified on both branches.
 * <p>
 * CLI Proxy for {@link RevertOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit revert <commit>...}
 * </ul>
 * 
 * @see RevertOp
 */
@Parameters(commandNames = "revert", commandDescription = "Revert commits to undo the changes made")
public class Revert extends AbstractCommand implements CLICommand {

    @Parameter(description = "<commits>...", variableArity = true)
    private List<String> commits = Lists.newArrayList();

    /**
     * Executes the revert command.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        final GeoGIT geogit = cli.getGeogit();
        checkState(geogit != null, "not in a geogit repository.");
        checkArgument(commits.size() > 0, "nothing specified for reverting");

        RevertOp revert = geogit.command(RevertOp.class);

        for (String st : commits) {
            Optional<ObjectId> commitId = geogit.command(RevParse.class).setRefSpec(st).call();
            checkState(commitId.isPresent(), "Couldn't resolve '" + st
                    + "' to a commit, aborting revert.");
            revert.addCommit(Suppliers.ofInstance(commitId.get()));
        }
        revert.call();

    }
}
