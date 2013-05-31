package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.api.porcelain.FetchOp;
import org.geogit.api.porcelain.FetchResult;
import org.geogit.api.porcelain.SynchronizationException;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

import com.google.common.collect.Lists;

public class FetchWebOp extends AbstractWebAPICommand {
    private boolean prune;

    private boolean fetchAll;

    private List<String> remotes = Lists.newArrayList();

    /**
     * Mutator for the prune variable
     * 
     * @param prune - true to prune remote tracking branches locally that no longer exist
     */
    public void setPrune(boolean prune) {
        this.prune = prune;
    }

    /**
     * Mutator for the fetchAll variable
     * 
     * @param fetchAll - true to fetch all
     */
    public void setFetchAll(boolean fetchAll) {
        this.fetchAll = fetchAll;
    }

    /**
     * Mutator for the remotes variable
     * 
     * @param remotes - a list of all the remotes to fetch from
     */
    public void setRemotes(List<String> remotes) {
        this.remotes = remotes;
    }

    /**
     * Adds a remote to the list of remote to fetch
     * 
     * @param remote - a remote to pull
     */
    public void addRemote(String remote) {
        this.remotes.add(remote);
    }

    @Override
    public void run(CommandContext context) {
        final CommandLocator geogit = this.getCommandLocator(context);

        FetchOp command = geogit.command(FetchOp.class);

        for (String remote : remotes) {
            command.addRemote(remote);
        }
        try {
            final FetchResult result = command.setAll(fetchAll).setPrune(prune).call();
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeFetchResponse(result);
                    out.finish();
                }
            });
        } catch (SynchronizationException e) {
            switch (e.statusCode) {
            case HISTORY_TOO_SHALLOW:
            default:
                context.setResponseContent(CommandResponse
                        .error("Unable to fetch, the remote history is shallow."));
            }
        }
    }

}
