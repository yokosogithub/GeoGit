<<<<<<< .merge_file_bbRLjC
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.porcelain.PushOp;
import org.geogit.api.porcelain.SynchronizationException;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

/**
 * Interface for the Push operation in GeoGit.
 * 
 * Web interface for {@link PushOp}
 */
public class PushWebOp extends AbstractWebAPICommand {
    private String remoteName;

    private boolean pushAll;

    private String refSpec;

    /**
     * Mutator for the remoteName variable
     * 
     * @param remoteName - the name of the remote to push to
     */
    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    /**
     * Mutator for the pushAll variable
     * 
     * @param pushAll - true to push all refs
     */
    public void setPushAll(boolean pushAll) {
        this.pushAll = pushAll;
    }

    /**
     * Mutator for the refSpec variable
     * 
     * @param refSpecs - the ref to push
     */
    public void setRefSpec(String refSpec) {
        this.refSpec = refSpec;
    }

    /**
     * Runs the command and builds the appropriate response.
     * 
     * @param context - the context to use for this command
     */
    @Override
    public void run(CommandContext context) {
        final CommandLocator geogit = this.getCommandLocator(context);

        PushOp command = geogit.command(PushOp.class);

        if (refSpec != null) {
            command.addRefSpec(refSpec);
        }

        try {
            command.setAll(pushAll).setRemote(remoteName).call();
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeElement("Push", "Success");
                    out.writeElement("dataPushed", "true");
                    out.finish();
                }
            });
        } catch (SynchronizationException e) {
            switch (e.statusCode) {
            case NOTHING_TO_PUSH:
                context.setResponseContent(new CommandResponse() {
                    @Override
                    public void write(ResponseWriter out) throws Exception {
                        out.start();
                        out.writeElement("Push", "Nothing to push.");
                        out.writeElement("dataPushed", "false");
                        out.finish();
                    }
                });
                break;
            case REMOTE_HAS_CHANGES:
                context.setResponseContent(CommandResponse
                        .error("Push failed: The remote repository has changes that would be lost in the event of a push."));
                break;
            case HISTORY_TOO_SHALLOW:
                context.setResponseContent(CommandResponse
                        .error("Push failed: There is not enough local history to complete the push."));
            }
        }
    }

}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import org.geogit.api.CommandLocator;
import org.geogit.api.porcelain.PushOp;
import org.geogit.api.porcelain.SynchronizationException;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

/**
 * Interface for the Push operation in GeoGit.
 * 
 * Web interface for {@link PushOp}
 */
public class PushWebOp extends AbstractWebAPICommand {
    private String remoteName;

    private boolean pushAll;

    private String refSpec;

    /**
     * Mutator for the remoteName variable
     * 
     * @param remoteName - the name of the remote to push to
     */
    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    /**
     * Mutator for the pushAll variable
     * 
     * @param pushAll - true to push all refs
     */
    public void setPushAll(boolean pushAll) {
        this.pushAll = pushAll;
    }

    /**
     * Mutator for the refSpec variable
     * 
     * @param refSpecs - the ref to push
     */
    public void setRefSpec(String refSpec) {
        this.refSpec = refSpec;
    }

    /**
     * Runs the command and builds the appropriate response.
     * 
     * @param context - the context to use for this command
     */
    @Override
    public void run(CommandContext context) {
        final CommandLocator geogit = this.getCommandLocator(context);

        PushOp command = geogit.command(PushOp.class);

        if (refSpec != null) {
            command.addRefSpec(refSpec);
        }

        try {
            final Boolean dataPushed = command.setAll(pushAll).setRemote(remoteName).call();
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeElement("Push", "Success");
                    out.writeElement("dataPushed", dataPushed.toString());
                    out.finish();
                }
            });
        } catch (SynchronizationException e) {
            switch (e.statusCode) {
            case REMOTE_HAS_CHANGES:
                context.setResponseContent(CommandResponse
                        .error("Push failed: The remote repository has changes that would be lost in the event of a push."));
                break;
            case HISTORY_TOO_SHALLOW:
                context.setResponseContent(CommandResponse
                        .error("Push failed: There is not enough local history to complete the push."));
            default:
                break;
            }
        }
    }

}
>>>>>>> .merge_file_xoxr7B
