/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;

/**
 * Interface for the Ls-Tree operation in GeoGit
 * 
 * Web interface for {@link LsTreeOp}
 */
public class LsTree extends AbstractWebAPICommand {

    boolean includeTrees;

    boolean onlyTrees;

    boolean recursive;

    boolean verbose;

    List<String> refList;

    /**
     * Mutator for the includeTrees variable
     * 
     * @param includeTrees - true to display trees in the response
     */
    public void setIncludeTrees(boolean includeTrees) {
        this.includeTrees = includeTrees;
    }

    /**
     * Mutator for the onlyTrees variable
     * 
     * @param onlyTrees - true to display only trees in the response
     */
    public void setOnlyTrees(boolean onlyTrees) {
        this.onlyTrees = onlyTrees;
    }

    /**
     * Mutator for the recursive variable
     * 
     * @param recursive - true to recurse through the trees
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Mutator for the verbose variable
     * 
     * @param verbose - true to print out the type, metadataId and Id of the object
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Mutator for the refList variable (Should this really be a list?)
     * 
     * @param refList - reference to start at
     */
    public void setRefList(List<String> refList) {
        this.refList = refList;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     */
    @Override
    public void run(CommandContext context) {
        String ref = null;
        if (refList != null && !refList.isEmpty()) {
            ref = refList.get(0);
        }
        LsTreeOp.Strategy lsStrategy = LsTreeOp.Strategy.CHILDREN;
        if (recursive) {
            if (includeTrees) {
                lsStrategy = LsTreeOp.Strategy.DEPTHFIRST;
            } else if (onlyTrees) {
                lsStrategy = LsTreeOp.Strategy.DEPTHFIRST_ONLY_TREES;
            } else {
                lsStrategy = LsTreeOp.Strategy.DEPTHFIRST_ONLY_FEATURES;
            }
        } else {
            if (onlyTrees) {
                lsStrategy = LsTreeOp.Strategy.TREES_ONLY;
            }
        }

        final CommandLocator geogit = this.getCommandLocator(context);

        final Iterator<NodeRef> iter = geogit.command(LsTreeOp.class).setReference(ref)
                .setStrategy(lsStrategy).call();

        context.setResponseContent(new CommandResponse() {

            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start(true);
                out.writeLsTreeResponse(iter, verbose);
                out.finish();
            }
        });

    }

}
