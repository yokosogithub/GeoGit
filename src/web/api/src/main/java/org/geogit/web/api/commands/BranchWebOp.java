/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.api.Ref;
import org.geogit.api.porcelain.BranchListOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

import com.google.common.collect.Lists;

/**
 * The interface for the Branch operations in GeoGit. Currently only supports listing of local and
 * remote branches.
 * 
 * Web interface for {@link BranchListOp}
 */

public class BranchWebOp extends AbstractWebAPICommand {

    private boolean list;

    private boolean remotes;

    /**
     * Mutator for the list option
     * 
     * @param list - true if you want to list any branches
     */
    public void setList(boolean list) {
        this.list = list;
    }

    /**
     * Mutator for the remote option
     * 
     * @param remotes - true if you want to list remote branches
     */
    public void setRemotes(boolean remotes) {
        this.remotes = remotes;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     */
    @Override
    public void run(CommandContext context) {
        if (list) {
            final CommandLocator geogit = this.getCommandLocator(context);
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
