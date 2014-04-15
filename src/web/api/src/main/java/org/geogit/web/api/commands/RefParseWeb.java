/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.Ref;
import org.geogit.api.plumbing.RefParse;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;

/**
 * Interface for the RefParse command in GeoGit
 * 
 * Web interface for {@link RefParse}
 */

public class RefParseWeb extends AbstractWebAPICommand {

    private String refSpec;

    /**
     * Mutator for the refSpec variable
     * 
     * @param name - the refSpec to parse
     */
    public void setName(String name) {
        this.refSpec = name;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (refSpec == null) {
            throw new CommandSpecException("No name was given.");
        }

        final CommandLocator geogit = this.getCommandLocator(context);
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
