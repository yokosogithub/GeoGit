/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli.plumbing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * Outputs the common ancestor of 2 commits
 * 
 */
@ReadOnly
@Parameters(commandNames = "merge-base", commandDescription = "Outputs the common ancestor of 2 commits")
public class MergeBase extends AbstractCommand implements CLICommand {

    /**
     * The commits to use for computing the common ancestor
     * 
     */
    @Parameter(description = "<commit> <commit>")
    private List<String> commits = new ArrayList<String>();

    @Override
    public void runInternal(GeogitCLI cli) throws IOException {
        checkParameter(commits.size() == 2, "Two commit references must be provided");

        ConsoleReader console = cli.getConsole();
        GeoGIT geogit = cli.getGeogit();

        Optional<RevObject> left = geogit.command(RevObjectParse.class).setRefSpec(commits.get(0))
                .call();
        checkParameter(left.isPresent(), commits.get(0) + " does not resolve to any object.");
        checkParameter(left.get() instanceof RevCommit, commits.get(0)
                + " does not resolve to a commit");
        Optional<RevObject> right = geogit.command(RevObjectParse.class).setRefSpec(commits.get(1))
                .call();
        checkParameter(right.isPresent(), commits.get(1) + " does not resolve to any object.");
        checkParameter(right.get() instanceof RevCommit, commits.get(1)
                + " does not resolve to a commit");
        Optional<RevCommit> ancestor = geogit.command(FindCommonAncestor.class)
                .setLeft((RevCommit) left.get()).setRight((RevCommit) right.get()).call();
        checkParameter(ancestor.isPresent(), "No common ancestor was found.");

        console.print(ancestor.get().getId().toString());
    }

}
