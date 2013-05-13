/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.IOException;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.porcelain.SquashOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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
     * Executes the log command using the provided options.
     * 
     * @param cli
     * @throws IOException
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        final Platform platform = cli.getPlatform();
        Preconditions.checkState(cli.getGeogit() != null, "Not a geogit repository: "
                + platform.pwd().getAbsolutePath());
        Preconditions.checkArgument(commits.size() == 2, "2 commit references must be supplied");

        final GeoGIT geogit = cli.getGeogit();

        Optional<ObjectId> sinceId = geogit.command(RevParse.class).setRefSpec(commits.get(0))
                .call();
        Preconditions.checkArgument(sinceId.isPresent(), "'since' reference cannot be found");
        Preconditions.checkArgument(geogit.getRepository().commitExists(sinceId.get()),
                "'since' reference does not resolve to a commit");
        RevCommit sinceCommit = geogit.getRepository().getCommit(sinceId.get());

        Optional<ObjectId> untilId = geogit.command(RevParse.class).setRefSpec(commits.get(1))
                .call();
        Preconditions.checkArgument(untilId.isPresent(), "'until' reference cannot be found");
        Preconditions.checkArgument(geogit.getRepository().commitExists(untilId.get()),
                "'until' reference does not resolve to a commit");
        RevCommit untilCommit = geogit.getRepository().getCommit(untilId.get());

        geogit.command(SquashOp.class).setSince(sinceCommit).setUntil(untilCommit)
                .setMessage(message).call();
    }

}
