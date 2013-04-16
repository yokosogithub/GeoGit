package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.Remote;
import org.geogit.api.porcelain.RemoteListOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

public class RemoteWebOp implements WebAPICommand {

    private boolean list;

    public void setList(boolean list) {
        this.list = list;
    }

    @Override
    public void run(CommandContext context) {
        if (list) {
            final GeoGIT geogit = context.getGeoGIT();
            final List<Remote> remotes = geogit.command(RemoteListOp.class).call();

            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeRemoteListResponse(remotes);
                    out.finish();
                }
            });
        }
    }

}
