package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.geogit.api.Remote;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.RemoteAddOp;
import org.geogit.api.porcelain.RemoteException;
import org.geogit.api.porcelain.RemoteRemoveOp;
import org.geogit.storage.fs.IniConfigDatabase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class RemoteRemoveOpTest {

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
    public void testNullName() {
        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        exception.expect(RemoteException.class);
        remoteRemove.setName(null).call();
    }

    @Test
    public void testEmptyName() {
        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        exception.expect(RemoteException.class);
        remoteRemove.setName("").call();
    }

    @Test
    public void testRemoveNoRemotes() {
        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        exception.expect(RemoteException.class);
        remoteRemove.setName("remote").call();
    }

    @Test
    public void testRemoveNonexistantRemote() {
        final RemoteAddOp remoteAdd = new RemoteAddOp(new IniConfigDatabase(testPlatform));

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        exception.expect(RemoteException.class);
        remoteRemove.setName("nonexistant").call();
    }

    @Test
    public void testRemoveRemote() {
        final RemoteAddOp remoteAdd = new RemoteAddOp(new IniConfigDatabase(testPlatform));

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(deletedRemote.getName(), remoteName);
        assertEquals(deletedRemote.getFetchURL(), remoteURL);
        assertEquals(deletedRemote.getPushURL(), remoteURL);
        assertEquals(deletedRemote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");
    }

    @Test
    public void testRemoveRemoteWithNoURL() {
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

        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(deletedRemote.getName(), remoteName);
        assertEquals(deletedRemote.getFetchURL(), "");
        assertEquals(deletedRemote.getPushURL(), "");
        assertEquals(deletedRemote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");
    }

    @Test
    public void testRemoveRemoteWithNoFetch() {
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

        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(deletedRemote.getName(), remoteName);
        assertEquals(deletedRemote.getFetchURL(), remoteURL);
        assertEquals(deletedRemote.getPushURL(), remoteURL);
        assertEquals(deletedRemote.getFetch(), "");
    }

    @Test
    public void testAccessorsAndMutators() {
        final RemoteRemoveOp remoteRemove = new RemoteRemoveOp(new IniConfigDatabase(testPlatform));

        String remoteName = "myremote";
        remoteRemove.setName(remoteName);
        assertEquals(remoteName, remoteRemove.getName());
    }
}
