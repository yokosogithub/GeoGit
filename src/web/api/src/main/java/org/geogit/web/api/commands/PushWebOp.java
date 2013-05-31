package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.api.porcelain.PushOp;
import org.geogit.api.porcelain.SynchronizationException;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

import com.google.common.collect.Lists;

public class PushWebOp extends AbstractWebAPICommand {
    private String remoteName;

    private boolean pushAll;

    private List<String> refSpecs = Lists.newArrayList();

    /**
     * Mutator for the remoteName variable
     * 
     * @param remoteName - the name of the remote to add or remove
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
     * Mutator for the refSpecs variable
     * 
     * @param refSpecs - a list of all the refs to pull
     */
    public void setRefSpecs(List<String> refSpecs) {
        this.refSpecs = refSpecs;
    }

    /**
     * Adds a ref to the list of refs to pull
     * 
     * @param refSpec - a ref to pull
     */
    public void addRefSpec(String refSpec) {
        this.refSpecs.add(refSpec);
    }

    @Override
    public void run(CommandContext context) {
        final CommandLocator geogit = this.getCommandLocator(context);

        PushOp command = geogit.command(PushOp.class);

        for (String refSpec : refSpecs) {
            command.addRefSpec(refSpec);
        }
        try {
            command.setAll(pushAll).setRemote(remoteName).call();
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeElement("Push", "Success");
                    out.finish();
                }
            });
        } catch (SynchronizationException e) {
            switch (e.statusCode) {
            case NOTHING_TO_PUSH:
                context.setResponseContent(CommandResponse.error("Nothing to push."));
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
