package org.geogit.web.api.commands;

import org.geogit.api.GeogitTransaction;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.RevParse;
import org.geogit.api.plumbing.merge.MergeScenarioReport;
import org.geogit.api.plumbing.merge.ReportMergeScenarioOp;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.api.porcelain.MergeOp.MergeReport;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

public class MergeWebOp extends AbstractWebAPICommand {

    private boolean noCommit;

    private String commit;

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

        final Optional<Ref> currHead = transaction.command(RefParse.class).setName(Ref.HEAD).call();
        if (!currHead.isPresent()) {
            throw new CommandSpecException("Repository has no HEAD, can't merge.");
        }

        MergeOp merge = transaction.command(MergeOp.class);

        try {
            final MergeReport report = merge.setNoCommit(noCommit).call();

            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeMergeResponse(report.getReport().get(), transaction, report.getOurs(),
                            report.getPairs().get(0).getTheirs(), report.getPairs().get(0)
                                    .getAncestor());
                    out.finish();
                }
            });
        } catch (Exception e) {
            final RevCommit ours = context.getGeoGIT().getRepository()
                    .getCommit(currHead.get().getObjectId());
            final Optional<ObjectId> oid = transaction.command(RevParse.class).setRefSpec(commit)
                    .call();
            final RevCommit theirs;
            if (oid.isPresent()) {
                theirs = context.getGeoGIT().getRepository().getCommit(oid.get());
                merge.addCommit(Suppliers.ofInstance(oid.get()));
            } else {
                throw new CommandSpecException("Couldn't resolve '" + commit + "' to a commit.");
            }

            final Optional<RevCommit> ancestor = transaction.command(FindCommonAncestor.class)
                    .setLeft(ours).setRight(theirs).call();
            context.setResponseContent(new CommandResponse() {
                final MergeScenarioReport report = transaction.command(ReportMergeScenarioOp.class)
                        .setMergeIntoCommit(ours).setToMergeCommit(theirs).call();

                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeMergeResponse(report, transaction, ours.getId(), theirs.getId(),
                            ancestor.get().getId());
                    out.finish();
                }
            });

        }
    }
}
