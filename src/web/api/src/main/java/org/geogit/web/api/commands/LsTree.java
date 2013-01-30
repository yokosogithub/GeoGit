package org.geogit.web.api.commands;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

/**
 *
 */
public class LsTree implements WebAPICommand {

    boolean includeTrees;

    boolean onlyTrees;

    boolean recursive;

    boolean verbose;

    List<String> refList;

    public void setIncludeTrees(boolean includeTrees) {
        this.includeTrees = includeTrees;
    }

    public void setOnlyTrees(boolean onlyTrees) {
        this.onlyTrees = onlyTrees;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setRefList(List<String> refList) {
        this.refList = refList;
    }

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

        final Iterator<NodeRef> iter = context.getGeoGIT().command(LsTreeOp.class)
                .setReference(ref).setStrategy(lsStrategy).call();

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
