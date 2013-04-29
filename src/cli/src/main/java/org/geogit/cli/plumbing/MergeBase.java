/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.plumbing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Outputs the common ancestor of 2 commits
 * 
 */
@Parameters(commandNames = "merge-base", commandDescription = "Outputs the common ancestor of 2 commits")
public class MergeBase implements CLICommand {

    /**
     * The commits to use for computing the common ancestor
     * 
     */
    @Parameter(description = "<commit> <commit>")
    private List<String> commits = new ArrayList<String>();

    /**
     * @param cli
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        checkArgument(commits.size() == 2, "Two commit references must be provided");

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        Optional<RevObject> left = geogit.command(RevObjectParse.class).setRefSpec(commits.get(0))
                .call();
        checkArgument(left.isPresent(), commits.get(0) + " does not resolve to any object.");
        checkArgument(left.get() instanceof RevCommit, commits.get(0)
                + " does not resolve to a commit");
        Optional<RevObject> right = geogit.command(RevObjectParse.class).setRefSpec(commits.get(0))
                .call();
        checkArgument(right.isPresent(), commits.get(0) + " does not resolve to any object.");
        checkArgument(right.get() instanceof RevCommit, commits.get(0)
                + " does not resolve to a commit");
        ;
        Optional<RevCommit> ancestor = geogit.command(FindCommonAncestor.class)
                .setLeft((RevCommit) left.get()).setRight((RevCommit) right.get()).call();
        checkArgument(ancestor.isPresent(), "No common ancestor was found.");

        console.print(ancestor.get().getId().toString());
    }

}
