/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.UpdateSymRef;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;

/**
 * Interface for the UpdateRef operation in the GeoGit.
 * 
 * Web interface for {@link UpdateRef}, {@link UpdateSymRef}
 */

public class UpdateRefWeb extends AbstractWebAPICommand {

    private String name;

    private String newValue;

    private boolean delete;

    /**
     * Mutator for the name variable
     * 
     * @param name - the name of the ref to update
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Mutator for the newValue variable
     * 
     * @param newValue - the new value to change the ref to
     */
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    /**
     * Mutator for the delete variable
     * 
     * @param delete - true to delete the ref
     */
    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    /**
     * Runs the command and builds the appropriate response.
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (name == null) {
            throw new CommandSpecException("No name was given.");
        } else if (!(delete) && newValue == null) {
            throw new CommandSpecException(
                    "Nothing specified to update with, must specify either deletion or new value to update to.");
        }

        final CommandLocator geogit = this.getCommandLocator(context);
        Optional<Ref> ref;

        try {
            ref = geogit.command(RefParse.class).setName(name).call();

            if (!ref.isPresent()) {
                throw new CommandSpecException("Invalid name: " + name);
            }

            if (ref.get() instanceof SymRef) {
                Optional<Ref> target = geogit.command(RefParse.class).setName(newValue).call();
                if (target.isPresent() && !(target.get() instanceof SymRef)) {
                    ref = geogit.command(UpdateSymRef.class).setDelete(delete).setName(name)
                            .setNewValue(target.get().getName()).call();
                } else {
                    throw new CommandSpecException("Invalid new target: " + newValue);
                }

            } else {
                Optional<ObjectId> target = geogit.command(RevParse.class).setRefSpec(newValue)
                        .call();
                if (target.isPresent()) {
                    ref = geogit.command(UpdateRef.class).setDelete(delete)
                            .setName(ref.get().getName()).setNewValue(target.get()).call();
                } else {
                    throw new CommandSpecException("Invalid new value: " + newValue);
                }
            }
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
                    out.writeUpdateRefResponse(newRef);
                    out.finish();
                }
            });
        }

    }

}
