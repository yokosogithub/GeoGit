package org.geogit.web.api.commands;

import java.util.Iterator;
import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
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


    public void setLimit(Integer parseInt) {

    }

    public void setOffset(Integer parseInt) {

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
//        if (!sinceUntilPaths.isEmpty()) {
//            List<String> sinceUntil = ImmutableList.copyOf((Splitter.on("..").split(sinceUntilPaths
//                    .get(0))));
//            Preconditions.checkArgument(sinceUntil.size() == 1 || sinceUntil.size() == 2,
//                    "Invalid refSpec format, expected [<until>]|[<since>..<until>]: %s",
//                    sinceUntilPaths.get(0));
//
//            String sinceRefSpec;
//            String untilRefSpec;
//            if (sinceUntil.size() == 1) {
//                // just until was given
//                sinceRefSpec = null;
//                untilRefSpec = sinceUntil.get(0);
//            } else {
//                sinceRefSpec = sinceUntil.get(0);
//                untilRefSpec = sinceUntil.get(1);
//            }
//            if (sinceRefSpec != null) {
//                Optional<ObjectId> since;
//                since = geogit.command(RevParse.class).setRefSpec(sinceRefSpec).call();
//                Preconditions.checkArgument(since.isPresent(), "Object not found '%s'",
//                        sinceRefSpec);
//                op.setSince(since.get());
//            }
//            if (untilRefSpec != null) {
//                Optional<ObjectId> until;
//                until = geogit.command(RevParse.class).setRefSpec(untilRefSpec).call();
//                Preconditions.checkArgument(until.isPresent(), "Object not found '%s'",
//                        sinceRefSpec);
//                op.setUntil(until.get());
//            }
//        }
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
