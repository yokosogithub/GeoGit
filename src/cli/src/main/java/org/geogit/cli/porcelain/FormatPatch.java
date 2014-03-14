/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.Patch;
import org.geogit.api.plumbing.diff.PatchSerializer;
import org.geogit.api.porcelain.CreatePatchOp;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.annotation.ReadOnly;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Store changes between two version of the repository in a patch file
 * 
 * Usage is identical to that of the diff command, but adding a destination file where the patch is
 * to be saved
 * 
 * @see Diff
 */
@ReadOnly
@Parameters(commandNames = "format-patch", commandDescription = "Creates a patch with a set of changes")
public class FormatPatch extends AbstractCommand implements CLICommand {

    @Parameter(description = "[<commit> [<commit>]] [-- <path>...]", arity = 2)
    private List<String> refSpec = Lists.newArrayList();

    @Parameter(names = "--", hidden = true, variableArity = true)
    private List<String> paths = Lists.newArrayList();

    @Parameter(names = { "-f", "--file" }, description = "The patch file")
    private String file;

    @Parameter(names = "--cached", description = "compares the specified tree (commit, branch, etc) and the staging area")
    private boolean cached;

    /**
     * Executes the format-patch command with the specified options.
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws IOException {
        checkParameter(refSpec.size() < 3, "Commit list is too long :%s", refSpec);

        GeoGIT geogit = cli.getGeogit();
        checkParameter(file != null, "Patch file not specified");

        DiffOp diff = geogit.command(DiffOp.class).setReportTrees(true);

        String oldVersion = resolveOldVersion();
        String newVersion = resolveNewVersion();

        diff.setOldVersion(oldVersion).setNewVersion(newVersion).setCompareIndex(cached);

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

        if (!entries.hasNext()) {
            cli.getConsole().println("No differences found");
            return;
        }

        Patch patch = geogit.command(CreatePatchOp.class).setDiffs(entries).call();
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");
        PatchSerializer.write(out, patch);

    }

    private String resolveOldVersion() {
        return refSpec.size() > 0 ? refSpec.get(0) : null;
    }

    private String resolveNewVersion() {
        return refSpec.size() > 1 ? refSpec.get(1) : null;
    }

}
