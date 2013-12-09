/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.ParseTimestamp;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.ResponseWriter;
import org.geotools.util.Range;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

/**
 * Interface for the Log operation in GeoGit.
 * 
 * Web interface for {@link LogOp}
 */
public class Log extends AbstractWebAPICommand {

    Integer skip;

    Integer limit;

    String since;

    String until;

    String sinceTime;

    String untilTime;

    List<String> paths;

    private int page;

    private int elementsPerPage;

    boolean firstParentOnly;

    boolean summarize = false;

    boolean returnRange = false;

    /**
     * Mutator for the limit variable
     * 
     * @param limit - the number of commits to print
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * Mutator for the offset variable
     * 
     * @param offset - the offset to start listing at
     */
    public void setOffset(Integer offset) {
        this.skip = offset;
    }

    /**
     * Mutator for the since variable
     * 
     * @param since - the start place to list commits
     */
    public void setSince(String since) {
        this.since = since;
    }

    /**
     * Mutator for the until variable
     * 
     * @param until - the end place for listing commits
     */
    public void setUntil(String until) {
        this.until = until;
    }

    /**
     * Mutator for the sinceTime variable
     * 
     * @param since - the start place to list commits
     */
    public void setSinceTime(String since) {
        this.sinceTime = since;
    }

    /**
     * Mutator for the untilTime variable
     * 
     * @param until - the end place for listing commits
     */
    public void setUntilTime(String until) {
        this.untilTime = until;
    }

    /**
     * Mutator for the paths variable
     * 
     * @param paths - list of paths to filter commits by
     */
    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    /**
     * Mutator for the page variable
     * 
     * @param page - the page number to build the response
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Mutator for the elementsPerPage variable
     * 
     * @param elementsPerPage - the number of elements to display in the response per page
     */
    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }

    /**
     * Mutator for the firstParentOnly variable
     * 
     * @param firstParentOnly - true to only show the first parent of a commit
     */
    public void setFirstParentOnly(boolean firstParentOnly) {
        this.firstParentOnly = firstParentOnly;
    }

    /**
     * Mutator for the summarize variable
     * 
     * @param summarize - if true, each commit will include a summary of changes from its first
     *        parent
     */
    public void setSummarize(boolean summarize) {
        this.summarize = summarize;
    }

    /**
     * Mutator for the returnRange variable.
     * 
     * @param returnRange - true to only show the first and last commit of the log, as well as a
     *        count of the commits in the range.
     */
    public void setReturnRange(boolean returnRange) {
        this.returnRange = returnRange;
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * 
     * @throws IllegalArgumentException
     */
    @Override
    public void run(CommandContext context) {
        final CommandLocator geogit = this.getCommandLocator(context);

        LogOp op = geogit.command(LogOp.class).setFirstParentOnly(firstParentOnly);

        if (skip != null) {
            op.setSkip(skip.intValue());
        }
        if (limit != null) {
            op.setLimit(limit.intValue());
        }

        if (this.sinceTime != null || this.untilTime != null) {
            Date since = new Date(0);
            Date until = new Date();
            if (this.sinceTime != null) {
                since = new Date(geogit.command(ParseTimestamp.class).setString(this.sinceTime)
                        .call());
            }
            if (this.untilTime != null) {
                until = new Date(geogit.command(ParseTimestamp.class).setString(this.untilTime)
                        .call());
            }
            op.setTimeRange(new Range<Date>(Date.class, since, until));
        }

        if (this.since != null) {
            Optional<ObjectId> since;
            since = geogit.command(RevParse.class).setRefSpec(this.since).call();
            Preconditions.checkArgument(since.isPresent(), "Object not found '%s'", this.since);
            op.setSince(since.get());
        }
        if (this.until != null) {
            Optional<ObjectId> until;
            until = geogit.command(RevParse.class).setRefSpec(this.until).call();
            Preconditions.checkArgument(until.isPresent(), "Object not found '%s'", this.until);
            op.setUntil(until.get());
        }
        if (paths != null && !paths.isEmpty()) {
            for (String path : paths) {
                op.addPath(path);
            }
        }

        final Iterator<RevCommit> log = op.call();

        Iterators.advance(log, page * elementsPerPage);

        if (summarize) {
            final String pathFilter;
            if (paths != null && !paths.isEmpty()) {
                pathFilter = paths.get(0);
            } else {
                pathFilter = null;
            }
            Function<RevCommit, CommitSummary> summaryFunctor = new Function<RevCommit, CommitSummary>() {

                @Override
                public CommitSummary apply(RevCommit input) {
                    ObjectId parent = ObjectId.NULL;
                    if (input.getParentIds().size() > 0) {
                        parent = input.getParentIds().get(0);
                    }
                    final Iterator<DiffEntry> diff = geogit.command(DiffOp.class)
                            .setOldVersion(parent).setNewVersion(input.getId())
                            .setFilter(pathFilter).call();

                    int added = 0;
                    int modified = 0;
                    int removed = 0;

                    while (diff.hasNext()) {
                        DiffEntry entry = diff.next();
                        if (entry.changeType() == DiffEntry.ChangeType.ADDED) {
                            added++;
                        } else if (entry.changeType() == DiffEntry.ChangeType.MODIFIED) {
                            modified++;
                        } else {
                            removed++;
                        }
                    }

                    return new CommitSummary(input, added, modified, removed);
                }
            };

            final Iterator<CommitSummary> summarizedLog = Iterators.transform(log, summaryFunctor);
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeSummarizedCommits(summarizedLog, elementsPerPage);
                    out.finish();
                }
            });
        } else {
            final boolean rangeLog = returnRange;
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeCommits(log, elementsPerPage, rangeLog);
                    out.finish();
                }
            });
        }

    }

    public class CommitSummary {
        private final RevCommit commit;

        private final int adds;

        private final int modifies;

        private final int removes;

        public CommitSummary(RevCommit commit, int adds, int modifies, int removes) {
            this.commit = commit;
            this.adds = adds;
            this.modifies = modifies;
            this.removes = removes;
        }

        public RevCommit getCommit() {
            return commit;
        }

        public int getAdds() {
            return adds;
        }

        public int getModifies() {
            return modifies;
        }

        public int getRemoves() {
            return removes;
        }
    }
}
