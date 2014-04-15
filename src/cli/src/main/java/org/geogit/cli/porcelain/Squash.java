/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.SquashOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Squashes a set of commits into a single one.
 * <p>
 * CLI proxy for {@link org.geogit.api.porcelain.SquashOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit squash [<message>] <since_commit> <until_commit>
 * </ul>
 * 
 * @see org.geogit.api.porcelain.LogOp
 */
@Parameters(commandNames = "squash", commandDescription = "Squash commits")
public class Squash extends AbstractCommand implements CLICommand {

    @Parameter(description = "<since_commit> <until_commit>", arity = 2)
    private List<String> commits = Lists.newArrayList();

    @Parameter(names = "-m", description = "Commit message")
    private String message;

    /**
     * Executes the squash command using the provided options.
     */
    @Override
    public void runInternal(GeogitCLI cli) {
        checkParameter(commits.size() == 2, "2 commit references must be supplied");

        final GeoGIT geogit = cli.getGeogit();

        Optional<ObjectId> sinceId = geogit.command(RevParse.class).setRefSpec(commits.get(0))
                .call();
        checkParameter(sinceId.isPresent(), "'since' reference cannot be found");
        checkParameter(geogit.getRepository().commitExists(sinceId.get()),
                "'since' reference does not resolve to a commit");
        RevCommit sinceCommit = geogit.getRepository().getCommit(sinceId.get());

        Optional<ObjectId> untilId = geogit.command(RevParse.class).setRefSpec(commits.get(1))
                .call();
        checkParameter(untilId.isPresent(), "'until' reference cannot be found");
        checkParameter(geogit.getRepository().commitExists(untilId.get()),
                "'until' reference does not resolve to a commit");
        RevCommit untilCommit = geogit.getRepository().getCommit(untilId.get());

        geogit.command(SquashOp.class).setSince(sinceCommit).setUntil(untilCommit)
                .setMessage(message).call();
    }

}
