package org.geogit.web.api.commands;

import java.util.Iterator;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.NothingToCommitException;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

/**
 *
 */
public class Commit implements WebAPICommand {

    String message;
    boolean all;

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public void run(CommandContext context) {
        if (message == null || message.trim().isEmpty()) {
            throw new CommandSpecException("No commit message provided");
        }
        final GeoGIT geogit = context.getGeoGIT();
        RevCommit commit;
        try {
            commit = geogit.command(CommitOp.class).setMessage(message)
                    .setAll(all).call();
            assert commit != null;
        } catch (NothingToCommitException noChanges) {
            context.setResponseContent(CommandResponse.warning("Nothing to commit"));
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
