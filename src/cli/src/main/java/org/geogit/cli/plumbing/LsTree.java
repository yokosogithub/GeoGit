/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.plumbing;

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 *
 */
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

    @Parameter(names = { "-a", "--abbrev" }, description = "Instead of showing the full 40-byte hexadecimal object lines, show only a partial prefix. "
            + "Non default number of digits can be specified with --abbrev <n>.")
    private Integer abbrev;

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        String ref;
        if (refList.isEmpty()) {
            ref = null;
        } else {
            ref = refList.get(0);
        }
        Iterator<NodeRef> iter = cli.getGeogit().command(LsTreeOp.class).setReference(ref)
                .setOnlyTrees(onlyTrees).setIncludeTrees(includeTrees).setRecursive(recursive)
                .call();

        Function<NodeRef, CharSequence> printFunctor = new Function<NodeRef, CharSequence>() {

            @Override
            public CharSequence apply(NodeRef input) {
                if (!verbose) {
                    return input.getPath();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(abbrev(input.getMetadataId())).append(" ")
                        .append(input.getType().toString().toLowerCase()).append(' ')
                        .append(abbrev(input.getObjectId())).append(' ').append(input.getPath());
                return sb;
            }

            private String abbrev(ObjectId oid) {
                return abbrev == null ? oid.toString() : oid.toString().substring(0,
                        abbrev.intValue());
            }
        };

        Iterator<CharSequence> lines = Iterators.transform(iter, printFunctor);
        final ConsoleReader console = cli.getConsole();

        while (lines.hasNext()) {
            console.println(lines.next());
        }
        console.flush();
    }
}
