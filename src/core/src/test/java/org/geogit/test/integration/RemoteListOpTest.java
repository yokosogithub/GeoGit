package org.geogit.test.integration;

import org.geogit.api.Remote;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.RemoteAddOp;
import org.geogit.api.porcelain.RemoteListOp;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public class RemoteListOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public final void setUpInternal() {
    }

    @Test
    public void testListNoRemotes() {
        final RemoteListOp remoteList = geogit.command(RemoteListOp.class);

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertTrue(allRemotes.isEmpty());
    }

    @Test
    public void testListMultipleRemotes() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

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

        final RemoteListOp remoteList = geogit.command(RemoteListOp.class);

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
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final ConfigOp config = geogit.command(ConfigOp.class);
        config.setAction(ConfigAction.CONFIG_UNSET).setName("remote." + remoteName + ".url").call();

        final RemoteListOp remoteList = geogit.command(RemoteListOp.class);

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertTrue(allRemotes.isEmpty());
    }

    @Test
    public void testListRemoteWithNoFetch() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final ConfigOp config = geogit.command(ConfigOp.class);
        config.setAction(ConfigAction.CONFIG_UNSET).setName("remote." + remoteName + ".fetch")
                .call();

        final RemoteListOp remoteList = geogit.command(RemoteListOp.class);

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertTrue(allRemotes.isEmpty());
    }
}
