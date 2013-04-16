package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.RevTag;
import org.geogit.api.porcelain.TagListOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

public class TagWebOp implements WebAPICommand {

    private boolean list;

    public void setList(boolean list) {
        this.list = list;
    }

    @Override
    public void run(CommandContext context) {
        if (list) {
            final GeoGIT geogit = context.getGeoGIT();
            final List<RevTag> tags = geogit.command(TagListOp.class).call();

            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeTagListResponse(tags);
                    out.finish();
                }
            });
        }
    }

}
