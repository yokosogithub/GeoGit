/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.plumbing;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.RevPerson;
import org.geogit.api.plumbing.ParseTimestamp;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geotools.util.Range;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Shows list of commits.
 * 
 * @see org.geogit.api.porcelain.LogOp
 */
@Parameters(commandNames = "rev-list", commandDescription = "Show list of commits")
public class RevList extends AbstractCommand implements CLICommand {

    @ParametersDelegate
    public final RevListArgs args = new RevListArgs();

    private GeoGIT geogit;

    private ConsoleReader console;

    /**
     * Executes the revlist command using the provided options.
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
        Preconditions.checkState(!args.commits.isEmpty(), "No starting commit provided");

        geogit = cli.getGeogit();

        LogOp op = geogit.command(LogOp.class).setTopoOrder(args.topo)
                .setFirstParentOnly(args.firstParent);

        for (String commit : args.commits) {
            Optional<ObjectId> commitId = geogit.command(RevParse.class).setRefSpec(commit).call();
            Preconditions.checkArgument(commitId.isPresent(), "Object not found '%s'", commit);
            Preconditions.checkArgument(geogit.getRepository().commitExists(commitId.get()),
                    "%s does not resolve to a commit", commit);
            op.addCommit(commitId.get());
        }
        if (args.author != null && !args.author.isEmpty()) {
            op.setAuthor(args.author);
        }
        if (args.committer != null && !args.committer.isEmpty()) {
            op.setCommiter(args.committer);
        }
        if (args.skip != null) {
            op.setSkip(args.skip.intValue());
        }
        if (args.limit != null) {
            op.setLimit(args.limit.intValue());
        }
        if (args.since != null || args.until != null) {
            Date since = new Date(0);
            Date until = new Date();
            if (args.since != null) {
                since = new Date(geogit.command(ParseTimestamp.class).setString(args.since).call());
            }
            if (args.until != null) {
                until = new Date(geogit.command(ParseTimestamp.class).setString(args.until).call());
            }
            op.setTimeRange(new Range<Date>(Date.class, since, until));
        }
        if (!args.pathNames.isEmpty()) {
            for (String s : args.pathNames) {
                op.addPath(s);
            }
        }
        Iterator<RevCommit> log = op.call();
        console = cli.getConsole();

        RawPrinter printer = new RawPrinter(args.changed);
        while (log.hasNext()) {
            printer.print(log.next());
            console.flush();
        }
    }

    private class RawPrinter {

        private boolean showChanges;

        public RawPrinter(boolean showChanges) {
            this.showChanges = showChanges;
        }

        public void print(RevCommit commit) throws IOException {

            StringBuilder sb = new StringBuilder();
            sb.append("commit ").append(commit.getId().toString()).append('\n');
            sb.append("tree ").append(commit.getTreeId().toString()).append('\n');
            sb.append("parent");
            for (ObjectId parentId : commit.getParentIds()) {
                sb.append(' ').append(parentId.toString());
            }
            sb.append('\n');
            sb.append("author ").append(format(commit.getAuthor())).append('\n');
            sb.append("committer ").append(format(commit.getCommitter())).append('\n');

            if (commit.getMessage() != null) {
                sb.append("message\n");
                sb.append("\t" + commit.getMessage().replace("\n", "\n\t"));
                sb.append('\n');
            }
            if (showChanges) {
                Iterator<DiffEntry> diff = geogit.command(DiffOp.class)
                        .setOldVersion(commit.parentN(0).or(ObjectId.NULL))
                        .setNewVersion(commit.getId()).call();
                DiffEntry diffEntry;
                sb.append("changes\n");
                while (diff.hasNext()) {
                    diffEntry = diff.next();
                    String path = diffEntry.newPath() != null ? diffEntry.newPath() : diffEntry
                            .oldPath();
                    sb.append('\t').append(path).append(' ')
                            .append(diffEntry.oldObjectId().toString()).append(' ')
                            .append(diffEntry.newObjectId().toString()).append('\n');
                }
            }
            console.println(sb.toString());
        }

        private String format(RevPerson p) {
            StringBuilder sb = new StringBuilder();
            sb.append(p.getName().or("[unknown]")).append(' ');
            sb.append(p.getEmail().or("[unknown]")).append(' ');
            sb.append(p.getTimestamp()).append(' ').append(p.getTimeZoneOffset());
            return sb.toString();
        }

    }

}