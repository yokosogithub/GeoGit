package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CloneOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.ConfigOp.ConfigScope;
import org.geogit.remote.RemoteRepositoryTestCase;
import org.junit.Test;
import org.neo4j.kernel.impl.util.FileUtils;
import org.opengis.feature.Feature;

import com.google.common.base.Throwables;

public class SparseCloneIntegrationTest extends RemoteRepositoryTestCase {

    private Feature points1_displaced;

    private Feature points2_displaced;

    private Feature points3_displaced;

    @Override
    protected void setUpInternal() throws Exception {
        points1_displaced = feature(pointsType, idP1, "StringProp1_1", new Integer(1000),
                "POINT(10 10)");
        points2_displaced = feature(pointsType, idP2, "StringProp1_2", new Integer(2000),
                "POINT(20 20)");
        points3_displaced = feature(pointsType, idP3, "StringProp1_3", new Integer(3000),
                "POINT(30 30)");
    }

    private void createRemoteRepo() throws Exception {
        ArrayList<RevCommit> commits = new ArrayList<RevCommit>();
        insertAndAdd(remoteGeogit.geogit, points1);
        commits.add(remoteGeogit.geogit.command(CommitOp.class).call());
        insertAndAdd(remoteGeogit.geogit, points2);
        commits.add(remoteGeogit.geogit.command(CommitOp.class).call());
        insertAndAdd(remoteGeogit.geogit, points3);
        commits.add(remoteGeogit.geogit.command(CommitOp.class).call());

        Iterator<RevCommit> logs = remoteGeogit.geogit.command(LogOp.class).call();
        List<RevCommit> logged = new ArrayList<RevCommit>();
        for (; logs.hasNext();) {
            logged.add(logs.next());
        }
    }

    private void setFilter(String filter) {
        URL envHome = new ResolveGeogitDir(localGeogit.geogit.getPlatform()).call();
        if (envHome == null) {
            throw new IllegalStateException("Not inside a geogit directory");
        }
        if (!"file".equals(envHome.getProtocol())) {
            throw new UnsupportedOperationException(
                    "Sparse clone works only against file system repositories. "
                            + "Repository location: " + envHome.toExternalForm());
        }
        File repoDir;
        try {
            repoDir = new File(envHome.toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        File newFilterFile = new File(repoDir, FILTER_FILE);

        FileUtils.copyFile(oldFilterFile, newFilterFile);
        cli.getGeogit().command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET)
                .setName("sparse.filter").setValue(FILTER_FILE).setScope(ConfigScope.LOCAL).call();
    }

    @Test
    public void testModificationThatTakesFeatureOutOfFilter(){
        createRemoteRepo();
        
        
        
        localGeogit.geogit.command(CloneOp.class).
        
    }
}
