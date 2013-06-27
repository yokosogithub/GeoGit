package org.geogit.web.api.commands;

import java.util.Iterator;

import org.geogit.api.CommandLocator;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

/**
 * Interface for the Commit operation in GeoGit.
 * 
 * Web interface for {@link CommitOp}
 */
public class Commit extends AbstractWebAPICommand {

    String message;

    boolean all;

    /**
     * Mutator for the message variable
     * 
     * @param message - the message for this commit
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Mutator for the all option
     * 
     * @param all - true to the commit everything in the working tree
     */
    public void setAll(boolean all) {
        this.all = all;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (this.getTransactionId() == null) {
            throw new CommandSpecException(
                    "No transaction was specified, commit requires a transaction to preserve the stability of the repository.");
        }
        final CommandLocator geogit = this.getCommandLocator(context);
        RevCommit commit;
        try {
            commit = geogit.command(CommitOp.class).setMessage(message).setAllowEmpty(true)
                    .setAll(all).call();
            assert commit != null;
        } catch (NothingToCommitException noChanges) {
            context.setResponseContent(CommandResponse.warning("Nothing to commit"));
            commit = null;
        } catch (IllegalStateException e) {
            context.setResponseContent(CommandResponse.warning(e.getMessage()));
            commit = null;
        }
        if (commit != null) {
            final ObjectId parentId = commit.parentN(0).or(ObjectId.NULL);
            final Iterator<DiffEntry> diff = geogit.command(DiffOp.class).setOldVersion(parentId)
                    .setNewVersion(commit.getId()).call();

            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeCommitResponse(diff);
                    out.finish();
                }
            });
        }
    }
}
