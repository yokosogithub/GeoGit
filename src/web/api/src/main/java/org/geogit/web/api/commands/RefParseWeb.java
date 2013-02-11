package org.geogit.web.api.commands;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.RefParse;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

import com.google.common.base.Optional;

public class RefParseWeb implements WebAPICommand {

    private String refSpec;

    public void setName(String name) {
        this.refSpec = name;
    }

    @Override
    public void run(CommandContext context) {
        if (refSpec == null) {
            throw new CommandSpecException("No name was given.");
        }

        final GeoGIT geogit = context.getGeoGIT();
        Optional<Ref> ref;

        try {
            ref = geogit.command(RefParse.class).setName(refSpec).call();
        } catch (Exception e) {
            context.setResponseContent(CommandResponse.error("Aborting UpdateRef: "
                    + e.getMessage()));
            return;
        }

        if (ref.isPresent()) {
            final Ref newRef = ref.get();
            context.setResponseContent(new CommandResponse() {

                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeRefParseResponse(newRef);
                    out.finish();
                }
            });
        } else {
            context.setResponseContent(new CommandResponse() {

                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeEmptyRefResponse();
                    out.finish();
                }
            });
        }
    }

}