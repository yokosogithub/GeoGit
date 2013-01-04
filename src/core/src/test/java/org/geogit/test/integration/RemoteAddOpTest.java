package org.geogit.test.integration;

import org.geogit.api.Remote;
import org.geogit.api.porcelain.RemoteAddOp;
import org.geogit.api.porcelain.RemoteException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RemoteAddOpTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public final void setUpInternal() {
    }

    @Test
    public void testNullName() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        exception.expect(RemoteException.class);
        remoteAdd.setName(null).setURL("http://test.com").call();
    }

    @Test
    public void testEmptyName() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        exception.expect(RemoteException.class);
        remoteAdd.setName("").setURL("http://test.com").call();
    }

    @Test
    public void testNullURL() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        exception.expect(RemoteException.class);
        remoteAdd.setName("myremote").setURL(null).call();
    }

    @Test
    public void testEmptyURL() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        exception.expect(RemoteException.class);
        remoteAdd.setName("myremote").setURL("").call();
    }

    @Test
    public void testAddRemoteNullBranch() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).setBranch(null).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");
    }

    @Test
    public void testAddRemoteEmptyBranch() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).setBranch("").call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");
    }

    @Test
    public void testAddRemoteWithBranch() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";
        String branch = "mybranch";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).setBranch(branch).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/" + branch + ":refs/remotes/" + remoteName
                + "/" + branch);
    }

    @Test
    public void testAddRemoteThatExists() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        exception.expect(RemoteException.class);
        remoteAdd.setName(remoteName).setURL("someotherurl.com").call();
    }

    @Test
    public void testAddMultipleRemotes() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName1 = "myremote";
        String remoteURL1 = "http://test.com";

        String remoteName2 = "myremote2";
        String remoteURL2 = "http://test2.org";

        Remote remote = remoteAdd.setName(remoteName1).setURL(remoteURL1).call();

        assertEquals(remote.getName(), remoteName1);
        assertEquals(remote.getFetchURL(), remoteURL1);
        assertEquals(remote.getPushURL(), remoteURL1);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName1 + "/*");

        remote = remoteAdd.setName(remoteName2).setURL(remoteURL2).call();

        assertEquals(remote.getName(), remoteName2);
        assertEquals(remote.getFetchURL(), remoteURL2);
        assertEquals(remote.getPushURL(), remoteURL2);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName2 + "/*");
    }

    @Test
    public void testAccessorsAndMutators() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";
        String branch = "mybranch";

        remoteAdd.setBranch(branch);
        assertEquals(branch, remoteAdd.getBranch());

        remoteAdd.setName(remoteName);
        assertEquals(remoteName, remoteAdd.getName());

        remoteAdd.setURL(remoteURL);
        assertEquals(remoteURL, remoteAdd.getURL());
    }

}
