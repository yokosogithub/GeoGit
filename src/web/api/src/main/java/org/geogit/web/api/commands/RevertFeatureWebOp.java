/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import static com.google.common.base.Preconditions.checkState;

import javax.annotation.Nullable;

import org.geogit.api.CommandLocator;
import org.geogit.api.CommitBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.api.plumbing.FindCommonAncestor;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.WriteBack;
import org.geogit.api.plumbing.merge.MergeScenarioReport;
import org.geogit.api.plumbing.merge.ReportMergeScenarioOp;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.api.porcelain.MergeOp.MergeReport;
import org.geogit.web.api.AbstractWebAPICommand;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

/**
 * The interface for the Add operation in GeoGit.
 * 
 * Web interface for {@link AddOp}
 */

public class RevertFeatureWebOp extends AbstractWebAPICommand {

    private String featurePath;

    private ObjectId oldCommitId;

    private ObjectId newCommitId;

    private Optional<String> authorName = Optional.absent();

    private Optional<String> authorEmail = Optional.absent();

    private Optional<String> commitMessage = Optional.absent();

    private Optional<String> mergeMessage = Optional.absent();

    /**
     * Mutator for the featurePath variable
     * 
     * @param featurePath - the path to the feature you want to revert
     */
    public void setPath(String featurePath) {
        this.featurePath = featurePath;
    }

    /**
     * Mutator for the oldCommitId variable
     * 
     * @param oldCommitId - the commit that contains the version of the feature to revert to
     */
    public void setOldCommitId(String oldCommitId) {
        this.oldCommitId = ObjectId.valueOf(oldCommitId);
    }

    /**
     * Mutator for the newCommitId variable
     * 
     * @param newCommitId - the commit that contains the version of the feature that we want to undo
     */
    public void setNewCommitId(String newCommitId) {
        this.newCommitId = ObjectId.valueOf(newCommitId);
    }

    /**
     * @param authorName the author of the merge commit
     */
    public void setAuthorName(@Nullable String authorName) {
        this.authorName = Optional.fromNullable(authorName);
    }

    /**
     * @param authorEmail the email of the author of the merge commit
     */
    public void setAuthorEmail(@Nullable String authorEmail) {
        this.authorEmail = Optional.fromNullable(authorEmail);
    }

    /**
     * @param commitMessage the commit message for the revert
     */
    public void setCommitMessage(@Nullable String commitMessage) {
        this.commitMessage = Optional.fromNullable(commitMessage);
    }

    /**
     * @param mergeMessage the message for the merge of the revert commit
     */
    public void setMergeMessage(@Nullable String mergeMessage) {
        this.mergeMessage = Optional.fromNullable(mergeMessage);
    }

    /**
     * Runs the command and builds the appropriate response
     * 
     * @param context - the context to use for this command
     * 
     * @throws CommandSpecException
     */
    @Override
    public void run(CommandContext context) {
        if (this.getTransactionId() == null) {
            throw new CommandSpecException(
                    "No transaction was specified, revert feature requires a transaction to preserve the stability of the repository.");
        }
        final CommandLocator geogit = this.getCommandLocator(context);

        Optional<RevTree> newTree = Optional.absent();
        Optional<RevTree> oldTree = Optional.absent();

        // get tree from new commit
        Optional<ObjectId> treeId = geogit.command(ResolveTreeish.class).setTreeish(newCommitId)
                .call();

        Preconditions.checkState(treeId.isPresent(),
                "New commit id did not resolve to a valid tree.");
        newTree = geogit.command(RevObjectParse.class).setRefSpec(treeId.get().toString())
                .call(RevTree.class);
        Preconditions.checkState(newTree.isPresent(), "Unable to read the new commit tree.");

        // get tree from old commit
        treeId = geogit.command(ResolveTreeish.class).setTreeish(oldCommitId).call();

        Preconditions.checkState(treeId.isPresent(),
                "Old commit id did not resolve to a valid tree.");
        oldTree = geogit.command(RevObjectParse.class).setRefSpec(treeId.get().toString())
                .call(RevTree.class);
        Preconditions.checkState(newTree.isPresent(), "Unable to read the old commit tree.");

        // get feature from old tree
        Optional<NodeRef> node = geogit.command(FindTreeChild.class).setParent(oldTree.get())
                .setIndex(true).setChildPath(featurePath).call();
        boolean delete = false;
        if (!node.isPresent()) {
            delete = true;
            node = geogit.command(FindTreeChild.class).setParent(newTree.get()).setIndex(true)
                    .setChildPath(featurePath).call();
            Preconditions.checkState(node.isPresent(),
                    "The feature was not found in either commit tree.");
        }

        // get the new parent tree
        ObjectId metadataId = ObjectId.NULL;
        Optional<NodeRef> parentNode = geogit.command(FindTreeChild.class).setParent(newTree.get())
                .setChildPath(node.get().getParentPath()).setIndex(true).call();

        RevTreeBuilder treeBuilder = null;
        if (parentNode.isPresent()) {
            metadataId = parentNode.get().getMetadataId();
            Optional<RevTree> parsed = geogit.command(RevObjectParse.class)
                    .setObjectId(parentNode.get().getNode().getObjectId()).call(RevTree.class);
            checkState(parsed.isPresent(), "Parent tree couldn't be found in the repository.");
            treeBuilder = new RevTreeBuilder(geogit.getIndex().getDatabase(), parsed.get());
            treeBuilder.remove(node.get().getNode().getName());
        } else {
            treeBuilder = new RevTreeBuilder(geogit.getIndex().getDatabase());
        }

        // put the old feature into the new tree
        if (!delete) {
            treeBuilder.put(node.get().getNode());
        }
        ObjectId newTreeId = geogit.command(WriteBack.class)
                .setAncestor(newTree.get().builder(geogit.getIndex().getDatabase()))
                .setChildPath(node.get().getParentPath()).setToIndex(true)
                .setTree(treeBuilder.build()).setMetadataId(metadataId).call();

        // build new commit with parent of new commit and the newly built tree
        CommitBuilder builder = new CommitBuilder();

        builder.setParentIds(Lists.newArrayList(newCommitId));
        builder.setTreeId(newTreeId);
        builder.setAuthor(authorName.orNull());
        builder.setAuthorEmail(authorEmail.orNull());
        builder.setMessage(commitMessage.or("Reverted changes made to " + featurePath + " at "
                + newCommitId.toString()));

        RevCommit mapped = builder.build();
        context.getGeoGIT().getRepository().getObjectDatabase().put(mapped);

        // merge commit into current branch
        final Optional<Ref> currHead = geogit.command(RefParse.class).setName(Ref.HEAD).call();
        if (!currHead.isPresent()) {
            throw new CommandSpecException("Repository has no HEAD, can't merge.");
        }

        MergeOp merge = geogit.command(MergeOp.class);
        merge.setAuthor(authorName.orNull(), authorEmail.orNull());
        merge.addCommit(Suppliers.ofInstance(mapped.getId()));
        merge.setMessage(mergeMessage.or("Merged revert of " + featurePath));

        try {
            final MergeReport report = merge.call();

            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeMergeResponse(Optional.fromNullable(report.getMergeCommit()), report
                            .getReport().get(), geogit, report.getOurs(), report.getPairs().get(0)
                            .getTheirs(), report.getPairs().get(0).getAncestor());
                    out.finish();
                }
            });
        } catch (Exception e) {
            final RevCommit ours = context.getGeoGIT().getRepository()
                    .getCommit(currHead.get().getObjectId());
            final RevCommit theirs = context.getGeoGIT().getRepository().getCommit(mapped.getId());
            final Optional<RevCommit> ancestor = geogit.command(FindCommonAncestor.class)
                    .setLeft(ours).setRight(theirs).call();
            context.setResponseContent(new CommandResponse() {
                final MergeScenarioReport report = geogit.command(ReportMergeScenarioOp.class)
                        .setMergeIntoCommit(ours).setToMergeCommit(theirs).call();

                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    Optional<RevCommit> mergeCommit = Optional.absent();
                    out.writeMergeResponse(mergeCommit, report, geogit, ours.getId(),
                            theirs.getId(), ancestor.get().getId());
                    out.finish();
                }
            });

        }
    }
}
