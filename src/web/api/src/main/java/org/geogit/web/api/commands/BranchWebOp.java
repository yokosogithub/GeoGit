package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.porcelain.BranchListOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

import com.google.common.collect.Lists;

public class BranchWebOp implements WebAPICommand {

    private boolean list;

    private boolean remotes;

    public void setList(boolean list) {
        this.list = list;
    }

    public void setRemotes(boolean remotes) {
        this.remotes = remotes;
    }

    @Override
    public void run(CommandContext context) {
        if (list) {
            final GeoGIT geogit = context.getGeoGIT();
            final List<Ref> localBranches = geogit.command(BranchListOp.class).call();
            final List<Ref> remoteBranches;
            if (remotes) {
                remoteBranches = geogit.command(BranchListOp.class).setLocal(false)
                        .setRemotes(remotes).call();
            } else {
                remoteBranches = Lists.newLinkedList();
            }
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeBranchListResponse(localBranches, remoteBranches);
                    out.finish();
                }
            });
        }
    }

}
