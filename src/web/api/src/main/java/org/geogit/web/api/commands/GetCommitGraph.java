package org.geogit.web.api.commands;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.web.api.CommandContext;
import org.geogit.web.api.CommandResponse;
import org.geogit.web.api.CommandSpecException;
import org.geogit.web.api.ResponseWriter;
import org.geogit.web.api.WebAPICommand;

import com.google.common.collect.Lists;

public class GetCommitGraph implements WebAPICommand {

    private String commitId;

    private int depth;

    private int page;

    private int elementsPerPage;

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }

    @Override
    public void run(CommandContext context) {
        if (commitId.equals(ObjectId.NULL.toString())) {
            throw new CommandSpecException("No commitId was given.");
        }
        final GeoGIT geogit = context.getGeoGIT();
        RevCommit commit = geogit.getRepository().getCommit(ObjectId.valueOf(commitId));
        final List<RevCommit> history = Lists.newLinkedList();

        List<CommitNode> nodes = Lists.newLinkedList();
        CommitNode node = new CommitNode(commit, 1);
        nodes.add(node);

        while (!nodes.isEmpty()) {
            node = nodes.remove(0);
            if (!history.contains(node.commit)) {
                history.add(node.commit);
            }
            if (this.depth == 0 || node.depth < this.depth) {
                for (ObjectId id : node.commit.getParentIds()) {
                    nodes.add(new CommitNode(geogit.getRepository().getCommit(id), node.depth + 1));
                }
            }
        }

        context.setResponseContent(new CommandResponse() {

            @Override
            public void write(ResponseWriter out) throws Exception {
                out.start();
                out.writeCommits(history.iterator(), page, elementsPerPage);
                out.finish();
            }
        });
    }

    private class CommitNode {
        public RevCommit commit;

        public int depth;

        CommitNode(RevCommit commit, int depth) {
            this.commit = commit;
            this.depth = depth;
        }
    }
}
