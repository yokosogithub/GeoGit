/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.porcelain.AddOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

/**
 * The interface for the Add operation in GeoGit.
 * 
 * Web interface for {@link AddOp}
 */

public class AddWebOp extends AbstractWebAPICommand {

    private String path;

    /**
     * Mutator for the path variable
     * 
     * @param path - the path to the feature you want to add
     */
    public void setPath(String path) {
        this.path = path;
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
        if (this.getTransactionId() == null) {
            throw new CommandSpecException(
                    "No transaction was specified, add requires a transaction to preserve the stability of the repository.");
        }
        final CommandLocator geogit = this.getCommandLocator(context);

        AddOp command = geogit.command(AddOp.class);

        if (path != null) {
            command.addPattern(path);
        }

        command.call();
        context.setResponseContent(new CommandResponse() {
            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeElement("Add", "Success");
                out.finish();
            }
        });
    }

}
