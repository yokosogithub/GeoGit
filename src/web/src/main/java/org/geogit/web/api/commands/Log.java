package org.geogit.web.api.commands;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.List;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.LogOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

/**
 *
 */
public class Log implements WebAPICommand {

    Integer skip;
    Integer limit;
    String since;
    String until;
    List<String> paths;

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public void setOffset(Integer offset) {
        this.skip = offset;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public void setUntil(String until) {
        this.until = until;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public void run(CommandContext context) {
        final GeoGIT geogit = context.getGeoGIT();

        LogOp op = geogit.command(LogOp.class);

        if (skip != null) {
            op.setSkip(skip.intValue());
        }
        if (limit != null) {
            op.setLimit(limit.intValue());
        }

        if (this.since != null) {
            Optional<ObjectId> since;
            since = geogit.command(RevParse.class).setRefSpec(this.since).call();
            Preconditions.checkArgument(since.isPresent(), "Object not found '%s'",
                    this.since);
            op.setSince(since.get());
        }
        if (this.until != null) {
            Optional<ObjectId> until;
            until = geogit.command(RevParse.class).setRefSpec(this.until).call();
            Preconditions.checkArgument(until.isPresent(), "Object not found '%s'",
                    this.until);
            op.setUntil(until.get());
        }
        if (paths != null && !paths.isEmpty()) {
            for (String path : paths) {
                op.addPath(path);
            }
        }

        final Iterator<RevCommit> log = op.call();
        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeCommits(log);
                out.finish();
            }
        });

    }
}
