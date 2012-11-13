package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.geogit.api.Remote;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.RemoteAddOp;
import org.geogit.api.porcelain.RemoteListOp;
import org.geogit.storage.fs.IniConfigDatabase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;

public class RemoteListOpTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    TestPlatform testPlatform;

    @Before
    public final void setUp() {
        final File userhome = tempFolder.newFolder("testUserHomeDir");
        final File workingDir = tempFolder.newFolder("testWorkingDir");
        tempFolder.newFolder("testWorkingDir/.geogit");
        testPlatform = new TestPlatform(workingDir);
        testPlatform.setUserHome(userhome);
    }

    @Test
    public void testListNoRemotes() {
        final RemoteListOp remoteList = new RemoteListOp(new IniConfigDatabase(testPlatform));

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertTrue(allRemotes.isEmpty());
    }

    @Test
    public void testListMultipleRemotes() {
        final RemoteAddOp remoteAdd = new RemoteAddOp(new IniConfigDatabase(testPlatform));

        String remoteName1 = "myremote";
        String remoteURL1 = "http://test.com";

        String remoteName2 = "myremote2";
        String remoteURL2 = "http://test2.org";
        String branch = "mybranch";

        Remote remote = remoteAdd.setName(remoteName1).setURL(remoteURL1).call();

        assertEquals(remote.getName(), remoteName1);
        assertEquals(remote.getFetchURL(), remoteURL1);
        assertEquals(remote.getPushURL(), remoteURL1);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName1 + "/*");

        remote = remoteAdd.setName(remoteName2).setURL(remoteURL2).setBranch(branch).call();

        assertEquals(remote.getName(), remoteName2);
        assertEquals(remote.getFetchURL(), remoteURL2);
        assertEquals(remote.getPushURL(), remoteURL2);
        assertEquals(remote.getFetch(), "+refs/heads/" + branch + ":refs/remotes/" + remoteName2
                + "/" + branch);

        final RemoteListOp remoteList = new RemoteListOp(new IniConfigDatabase(testPlatform));

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertEquals(2, allRemotes.size());

        Remote firstRemote = allRemotes.get(0);
        Remote secondRemote = allRemotes.get(1);

        if (!firstRemote.getName().equals(remoteName1)) {
            // swap first and second
            Remote tempRemote = firstRemote;
            firstRemote = secondRemote;
            secondRemote = tempRemote;
        }

        assertEquals(firstRemote.getName(), remoteName1);
        assertEquals(firstRemote.getFetchURL(), remoteURL1);
        assertEquals(firstRemote.getPushURL(), remoteURL1);
        assertEquals(firstRemote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName1 + "/*");

        assertEquals(secondRemote.getName(), remoteName2);
        assertEquals(secondRemote.getFetchURL(), remoteURL2);
        assertEquals(secondRemote.getPushURL(), remoteURL2);
        assertEquals(secondRemote.getFetch(), "+refs/heads/" + branch + ":refs/remotes/"
                + remoteName2 + "/" + branch);
    }

    @Test
    public void testListRemoteWithNoURL() {
        final RemoteAddOp remoteAdd = new RemoteAddOp(new IniConfigDatabase(testPlatform));

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));
        config.setAction(ConfigAction.CONFIG_UNSET).setName("remote." + remoteName + ".url").call();

        final RemoteListOp remoteList = new RemoteListOp(new IniConfigDatabase(testPlatform));

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertTrue(allRemotes.isEmpty());
    }

    @Test
    public void testListRemoteWithNoFetch() {
        final RemoteAddOp remoteAdd = new RemoteAddOp(new IniConfigDatabase(testPlatform));

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));
        config.setAction(ConfigAction.CONFIG_UNSET).setName("remote." + remoteName + ".fetch")
                .call();

        final RemoteListOp remoteList = new RemoteListOp(new IniConfigDatabase(testPlatform));

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertTrue(allRemotes.isEmpty());
    }
}
