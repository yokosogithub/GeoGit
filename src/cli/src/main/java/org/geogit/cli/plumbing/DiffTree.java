/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.plumbing;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Plumbing command to shows changes between commits
 * 
 * @see DiffOp
 */
@Parameters(commandNames = "diff-tree", commandDescription = "Show changes between commits")
public class DiffTree extends AbstractCommand implements CLICommand {

    @Parameter(description = "[<commit> [<commit>]] [-- <path>...]", arity = 2)
    private List<String> refSpec = Lists.newArrayList();

    @Parameter(names = "--", hidden = true, variableArity = true)
    private List<String> paths = Lists.newArrayList();

    /**
     * Executes the diff-tree command with the specified options.
     * 
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (refSpec.size() > 2) {
            cli.getConsole().println("Commit list is too long :" + refSpec);
            return;
        }

        GeoGIT geogit = cli.getGeogit();

        DiffOp diff = geogit.command(DiffOp.class);

        String oldVersion = resolveOldVersion();
        String newVersion = resolveNewVersion();

        diff.setOldVersion(oldVersion).setNewVersion(newVersion);

        Iterator<DiffEntry> entries;
        if (paths.isEmpty()) {
            entries = diff.setProgressListener(cli.getProgressListener()).call();
        } else {
            entries = Iterators.emptyIterator();
            for (String path : paths) {
                Iterator<DiffEntry> moreEntries = diff.setFilter(path)
                        .setProgressListener(cli.getProgressListener()).call();
                entries = Iterators.concat(entries, moreEntries);
            }
        }

        DiffEntry entry;
        while (entries.hasNext()) {
            entry = entries.next();
            StringBuilder sb = new StringBuilder();
            String path = entry.newPath() != null ? entry.newPath() : entry.oldPath();
            sb.append(path).append(" ").append(entry.oldObjectId().toString()).append(" ")
                    .append(entry.newObjectId().toString());
            cli.getConsole().println(sb.toString());
        }
    }

    private String resolveOldVersion() {
        return refSpec.size() > 0 ? refSpec.get(0) : null;
    }

    private String resolveNewVersion() {
        return refSpec.size() > 1 ? refSpec.get(1) : null;
    }

}
