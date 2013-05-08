package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.GeogitTransaction;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.merge.CheckMergeScenarioOp;
import org.geogit.api.plumbing.merge.MergeScenarioReport;
import org.geogit.api.plumbing.merge.ReportMergeScenarioOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

public class MergeWebOp extends AbstractWebAPICommand {

    private boolean checkConflicts;

    private boolean dryRun;

    private boolean noCommit;

    private String commit;

    private String baseRef;

    /**
     * Mutator for the checkConflicts variable
     * 
     * @param checkConflicts - true to check for conflicts only
     */
    public void setCheckConflicts(boolean checkConflicts) {
        this.checkConflicts = checkConflicts;
    }

    /**
     * Mutator for the dryRun variable
     * 
     * @param dryRun - true to run a dry run of merge to show the results without actually merging
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Mutator for the noCommit variable
     * 
     * @param noCommit - true to merge without creating a commit afterwards
     */
    public void setNoCommit(boolean noCommit) {
        this.noCommit = noCommit;
    }

    /**
     * Mutator for the commit variable
     * 
     * @param commit - the commit to merge into the baseRef or the currently checked out ref
     */
    public void setCommit(String commit) {
        this.commit = commit;
    }

    /**
     * Mutator for the baseRef variable
     * 
     * @param baseRef - the baseRef to merge into
     */
    public void setBaseRef(String baseRef) {
        this.baseRef = baseRef;
    }

    /**
     * Runs the command and builds the appropriate response.
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (this.getTransactionId() == null) {
            throw new CommandSpecException(
                    "No transaction was specified, merge requires a transaction to preserve the stability of the repository.");
        } else if (this.commit == null) {
            throw new CommandSpecException("No commits were specified for merging.");
        }

        final GeogitTransaction transaction = (GeogitTransaction) this.getCommandLocator(context);

        Optional<Ref> currHead = transaction.command(RefParse.class).setName(Ref.HEAD).call();
        if (!currHead.isPresent()) {
            throw new CommandSpecException("Repository has no HEAD, can't merge.");
        }

        final String target = ((SymRef) currHead.get()).getTarget();

        if (baseRef != null) {
            Optional<Ref> base = transaction.command(RefParse.class).setName(baseRef).call();
            if (base.isPresent()) {
                transaction.command(CheckoutOp.class).setSource(base.get().getName()).call();
                currHead = base;
            } else {
                throw new CommandSpecException("Couldn't resolve '" + baseRef + "' to a ref.");
            }
        }

        try {
            if (checkConflicts) {
                CheckMergeScenarioOp command = transaction.command(CheckMergeScenarioOp.class);
                List<RevCommit> commitsToMerge = Lists.newArrayList();
                commitsToMerge.add(context.getGeoGIT().getRepository()
                        .getCommit(currHead.get().getObjectId()));

                Optional<ObjectId> oid = transaction.command(RevParse.class).setRefSpec(commit)
                        .call();
                if (oid.isPresent()) {
                    commitsToMerge.add(context.getGeoGIT().getRepository().getCommit(oid.get()));
                } else {
                    throw new CommandSpecException("Couldn't resolve '" + commit + "' to a commit.");
                }

                final boolean conflicts = command.setCommits(commitsToMerge).call();

                context.setResponseContent(new CommandResponse() {

                    @Override
                    public void write(ResponseWriter out) throws Exception {
                        out.start();
                        out.writeElement("conflicts", Boolean.toString(conflicts));
                        out.finish();
                    }
                });
            } else if (dryRun) {
                ReportMergeScenarioOp command = transaction.command(ReportMergeScenarioOp.class);
                final RevCommit ours = context.getGeoGIT().getRepository()
                        .getCommit(currHead.get().getObjectId());
                command.setMergeIntoCommit(ours);
                final Optional<ObjectId> oid = transaction.command(RevParse.class)
                        .setRefSpec(commit).call();
                final RevCommit theirs;
                if (oid.isPresent()) {
                    theirs = context.getGeoGIT().getRepository().getCommit(oid.get());
                    command.setToMergeCommit(theirs);
                } else {
                    throw new CommandSpecException("Couldn't resolve '" + commit + "' to a commit.");
                }

                final MergeScenarioReport report = command.call();

                final Optional<RevCommit> ancestor = transaction.command(FindCommonAncestor.class)
                        .setLeft(ours).setRight(theirs).call();

                context.setResponseContent(new CommandResponse() {

                    @Override
                    public void write(ResponseWriter out) throws Exception {
                        out.start();
                        out.writeMergeDryRunResponse(report, transaction, ours.getId(),
                                theirs.getId(), ancestor.get().getId());
                        out.finish();
                    }
                });
            } else {
                MergeOp merge = transaction.command(MergeOp.class);

                Optional<ObjectId> oid = transaction.command(RevParse.class).setRefSpec(commit)
                        .call();
                if (oid.isPresent()) {
                    merge.addCommit(Suppliers.ofInstance(oid.get()));
                } else {
                    throw new CommandSpecException("Couldn't resolve '" + commit + "' to a commit.");
                }

                final RevCommit commit = merge.setNoCommit(noCommit).call();

                context.setResponseContent(new CommandResponse() {

                    @Override
                    public void write(ResponseWriter out) throws Exception {
                        out.start();
                        out.writeMergeResponse(commit);
                        out.finish();
                    }
                });
            }
        } catch (Exception e) {
            if (baseRef != null) {
                transaction.command(CheckoutOp.class).setSource(target).call();
            }
            context.setResponseContent(CommandResponse.error("Aborting UpdateRef: "
                    + e.getMessage()));
            return;
        }

        if (baseRef != null) {
            transaction.command(CheckoutOp.class).setSource(target).call();
        }
    }
}
