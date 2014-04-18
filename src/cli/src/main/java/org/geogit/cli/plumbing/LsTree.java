<<<<<<< .merge_file_psiZF4
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.plumbing;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.NodeRef;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 *
 */
@ReadOnly
@Parameters(commandNames = "ls-tree", commandDescription = "Obtain information about features in the index and the working tree.")
public class LsTree extends AbstractCommand implements CLICommand {

    @Parameter(description = "<[refspec]:[path]>", arity = 1)
    private List<String> refList = Lists.newArrayList();

    @Parameter(names = { "-t" }, description = "Show tree entries even when going to recurse them. Has no effect if -r was not passed. -d implies -t.")
    private boolean includeTrees;

    @Parameter(names = { "-d" }, description = "Show only the named tree entry itself, not its children.")
    private boolean onlyTrees;

    @Parameter(names = { "-r" }, description = "Recurse into sub-trees.")
    private boolean recursive;

    @Parameter(names = { "-v", "--verbose" }, description = "Verbose output, include metadata, object id, and object type among object path.")
    private boolean verbose;

    @Override
    public void runInternal(GeogitCLI cli) throws IOException {
        String ref;
        if (refList.isEmpty()) {
            ref = null;
        } else {
            ref = refList.get(0);
        }
        Strategy lsStrategy = Strategy.CHILDREN;
        if (recursive) {
            if (includeTrees) {
                lsStrategy = Strategy.DEPTHFIRST;
            } else if (onlyTrees) {
                lsStrategy = Strategy.DEPTHFIRST_ONLY_TREES;
            } else {
                lsStrategy = Strategy.DEPTHFIRST_ONLY_FEATURES;
            }
        } else {
            if (onlyTrees) {
                lsStrategy = Strategy.TREES_ONLY;
            }
        }
        Iterator<NodeRef> iter = cli.getGeogit().command(LsTreeOp.class).setReference(ref)
                .setStrategy(lsStrategy).call();

        final ConsoleReader console = cli.getConsole();

        Function<NodeRef, CharSequence> printFunctor = new Function<NodeRef, CharSequence>() {

            @Override
            public CharSequence apply(NodeRef input) {
                if (!verbose) {
                    return input.path();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(input.getMetadataId().toString()).append(' ')
                        .append(input.getType().toString().toLowerCase()).append(' ')
                        .append(input.objectId().toString()).append(' ').append(input.path());
                return sb;
            }
        };

        Iterator<CharSequence> lines = Iterators.transform(iter, printFunctor);

        while (lines.hasNext()) {
            console.println(lines.next());
        }
        console.flush();
    }
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.plumbing;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.NodeRef;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;

/**
 *
 */
@ReadOnly
@Parameters(commandNames = "ls-tree", commandDescription = "Obtain information about features in the index and the working tree.")
public class LsTree extends AbstractCommand implements CLICommand {

    @Parameter(description = "<[refspec]:[path]>", arity = 1)
    private List<String> refList = Lists.newArrayList();

    @Parameter(names = { "-t" }, description = "Show tree entries even when going to recurse them. Has no effect if -r was not passed. -d implies -t.")
    private boolean includeTrees;

    @Parameter(names = { "-d" }, description = "Show only the named tree entry itself, not its children.")
    private boolean onlyTrees;

    @Parameter(names = { "-r" }, description = "Recurse into sub-trees.")
    private boolean recursive;

    @Parameter(names = { "-v", "--verbose" }, description = "Verbose output, include metadata, object id, and object type along with object path.")
    private boolean verbose;

    @Override
    public void runInternal(final GeogitCLI cli) throws IOException {
        String ref;
        if (refList.isEmpty()) {
            ref = null;
        } else {
            ref = refList.get(0);
        }
        Strategy lsStrategy = Strategy.CHILDREN;
        if (recursive) {
            if (includeTrees) {
                lsStrategy = Strategy.DEPTHFIRST;
            } else if (onlyTrees) {
                lsStrategy = Strategy.DEPTHFIRST_ONLY_TREES;
            } else {
                lsStrategy = Strategy.DEPTHFIRST_ONLY_FEATURES;
            }
        } else {
            if (onlyTrees) {
                lsStrategy = Strategy.TREES_ONLY;
            }
        }
        Iterator<NodeRef> iter = cli.getGeogit().command(LsTreeOp.class).setReference(ref)
                .setStrategy(lsStrategy).call();

        final ConsoleReader console = cli.getConsole();

        Function<NodeRef, CharSequence> printFunctor = new Function<NodeRef, CharSequence>() {

            @Override
            public CharSequence apply(NodeRef input) {
                StringBuilder sb = new StringBuilder();
                if (!verbose) {
                    sb.append(input.path());
                } else {
                    Envelope env = new Envelope();
                    input.getNode().expand(env);
                    StringBuilder sbenv = new StringBuilder();
                    sbenv.append(Double.toString(env.getMinX())).append(";")
                            .append(Double.toString(env.getMaxX())).append(";")
                            .append(Double.toString(env.getMinY())).append(";")
                            .append(Double.toString(env.getMaxY()));
                    sb.append(input.getMetadataId().toString()).append(' ')
                            .append(input.getType().toString().toLowerCase()).append(' ')
                            .append(input.objectId().toString()).append(' ').append(input.path())
                            .append(' ').append(sbenv);
                    if (input.getType().equals(TYPE.TREE)) {
                        RevTree tree = cli.getGeogit().command(RevObjectParse.class)
                                .setObjectId(input.objectId()).call(RevTree.class).get();
                        sb.append(' ').append(tree.size()).append(' ').append(tree.numTrees());
                    }
                }
                return sb;
            }
        };

        Iterator<CharSequence> lines = Iterators.transform(iter, printFunctor);

        while (lines.hasNext()) {
            console.println(lines.next());
        }
        console.flush();
    }
}
>>>>>>> .merge_file_Orhhxd
