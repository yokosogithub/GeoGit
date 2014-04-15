/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.performance;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

public class LogOpPerformanceTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Ignore
    @Test
    public void testCommits() throws Exception {
        System.err.println("############### Warming up....");
        createAndLogMultipleCommits(1000);
        System.err.println("############### Warm up done.");

        createAndLogMultipleCommits(1000);
        createAndLogMultipleCommits(1000 * 10);
        // createAndLogMultipleCommits(1000 * 100);
        // createAndLogMultipleCommits(1000 * 1000);
    }

    @Ignore
    @Test
    public void testBranches() throws Exception {
        createAndLogMultipleBranches(200, 200);
    }

    private void createAndLogMultipleBranches(int numBranches, int numCommits) throws Exception {
        super.doSetUp();

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        System.err.println("***********\nCreating " + numberFormat.format(numBranches)
                + " branches with " + numberFormat.format(numCommits) + " commits each...");

        Stopwatch sw = new Stopwatch().start();
        List<ObjectId> ids = createBranches(numBranches, numCommits);
        sw.stop();
        System.err.println(numberFormat.format(numBranches) + " branches with "
                + numberFormat.format(numCommits) + " comits each created in " + sw.toString());
        System.err.flush();

        LogOp op = geogit.command(LogOp.class);
        for (ObjectId id : ids) {
            op.addCommit(id);
        }
        sw.reset().start();
        Iterator<RevCommit> commits = op.call();
        sw.stop();
        System.err.println("LogOp took " + sw.toString());

        benchmarkIteration(commits);

        op = geogit.command(LogOp.class).setTopoOrder(true);
        for (ObjectId id : ids) {
            op.addCommit(id);
        }
        sw.reset().start();
        commits = op.call();
        sw.stop();
        System.err.println("LogOp using --topo-order took " + sw.toString());
        benchmarkIteration(commits);

        super.tearDown();
    }

    private void createAndLogMultipleCommits(int numCommits) throws Exception {
        super.doSetUp();

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        System.err.println("***********\nCreating " + numberFormat.format(numCommits)
                + " commits...");

        Stopwatch sw = new Stopwatch().start();
        createCommits(numCommits, "");
        sw.stop();
        System.err.println(numberFormat.format(numCommits) + " created in " + sw.toString());
        System.err.flush();

        sw.reset().start();
        Iterator<RevCommit> commits = geogit.command(LogOp.class).call();
        sw.stop();
        System.err.println("LogOp took " + sw.toString());

        benchmarkIteration(commits);

        super.tearDown();
    }

    private RevCommit createCommits(int numCommits, String branchName) {
        int largeStep = numCommits / 10;
        int smallStep = numCommits / 100;

        RevCommit commit = null;
        for (int i = 1; i <= numCommits; i++) {
            if (i % largeStep == 0) {
                System.err.print(i);
                System.err.flush();
            } else if (i % smallStep == 0) {
                System.err.print('.');
                System.err.flush();
            }
            commit = geogit.command(CommitOp.class).setAllowEmpty(true)
                    .setMessage("Commit " + i + " in branch " + branchName).call();
        }
        System.err.print('\n');
        return commit;
    }

    private List<ObjectId> createBranches(int numBranches, int numCommits) {
        List<ObjectId> list = Lists.newArrayList();
        for (int i = 1; i <= numBranches; i++) {
            String branchName = "branch" + Integer.toString(i);
            geogit.command(CommitOp.class).setAllowEmpty(true)
                    .setMessage("Commit before " + branchName).call();
            geogit.command(BranchCreateOp.class).setAutoCheckout(true).setName(branchName).call();
            createCommits(numCommits / 2, branchName);
            geogit.command(CheckoutOp.class).setSource(Ref.MASTER).call();
            geogit.command(CommitOp.class).setAllowEmpty(true)
                    .setMessage("Commit during " + branchName).call();
            geogit.command(CheckoutOp.class).setSource(branchName).call();
            RevCommit lastCommit = createCommits(numCommits / 2, branchName);
            geogit.command(CheckoutOp.class).setSource(Ref.MASTER).call();
            list.add(lastCommit.getId());
            // System.err.println("branch " + Integer.toString(i));
        }
        return list;
    }

    private void benchmarkIteration(Iterator<RevCommit> commits) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        Stopwatch sw = new Stopwatch();
        sw.reset().start();
        int c = 0;
        while (commits.hasNext()) {
            c++;
            commits.next();
        }
        sw.stop();
        System.err.println("Iterated " + numberFormat.format(c) + " commits in " + sw.toString());
    }

}
