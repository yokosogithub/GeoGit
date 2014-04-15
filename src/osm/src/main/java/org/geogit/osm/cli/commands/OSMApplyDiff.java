/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CommandFailedException;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.internal.OSMApplyDiffOp;
import org.geogit.osm.internal.OSMReport;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * Imports data from an OSM file
 */
@Parameters(commandNames = "apply-diff", commandDescription = "Apply a OSM diff file to OSM data in the current repo")
public class OSMApplyDiff extends AbstractCommand implements CLICommand {

    @Parameter(arity = 1, description = "OSM diff file path", required = true)
    public List<String> diffFilepath = Lists.newArrayList();

    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        checkParameter(diffFilepath != null && diffFilepath.size() == 1,
                "One file must be specified");
        File diffFile = new File(diffFilepath.get(0));
        checkParameter(diffFile.exists(), "The specified OSM diff file does not exist");

        try {
            Optional<OSMReport> report = cli.getGeogit().command(OSMApplyDiffOp.class)
                    .setDiffFile(diffFile).setProgressListener(cli.getProgressListener()).call();
            if (report.isPresent()) {
                OSMReport rep = report.get();
                String msg;
                if (rep.getUnpprocessedCount() > 0) {
                    msg = String
                            .format("\nSome diffs from the specified file were not applied.\n"
                                    + "Processed entities: %,d.\n %,d.\nNodes: %,d.\nWays: %,d.\n Elements not applied:",
                                    rep.getCount(), rep.getUnpprocessedCount(), rep.getNodeCount(),
                                    rep.getWayCount());
                } else {
                    msg = String.format("\nProcessed entities: %,d.\n Nodes: %,d.\n Ways: %,d\n",
                            rep.getCount(), rep.getNodeCount(), rep.getWayCount());
                }
                cli.getConsole().println(msg);
            }

        } catch (RuntimeException e) {
            throw new CommandFailedException("Error importing OSM data: " + e.getMessage(), e);
        }

    }
}
