/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
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

        assertEquals(remoteName1, remote.getName());
        assertEquals(remoteURL1, remote.getFetchURL());
        assertEquals(remoteURL1, remote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName1 + "/*", remote.getFetch());

        remote = remoteAdd.setName(remoteName2).setURL(remoteURL2).setBranch(branch).call();

        assertEquals(remoteName2, remote.getName());
        assertEquals(remoteURL2, remote.getFetchURL());
        assertEquals(remoteURL2, remote.getPushURL());
        assertEquals("+refs/heads/" + branch + ":refs/remotes/" + remoteName2 + "/" + branch,
                remote.getFetch());

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

        assertEquals(remoteName1, firstRemote.getName());
        assertEquals(remoteURL1, firstRemote.getFetchURL());
        assertEquals(remoteURL1, firstRemote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName1 + "/*", firstRemote.getFetch());

        assertEquals(remoteName2, secondRemote.getName());
        assertEquals(remoteURL2, secondRemote.getFetchURL());
        assertEquals(remoteURL2, secondRemote.getPushURL());
        assertEquals("+refs/heads/" + branch + ":refs/remotes/" + remoteName2 + "/" + branch,
                secondRemote.getFetch());
    }

    @Test
    public void testListRemoteWithNoURL() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", remote.getFetch());

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

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", remote.getFetch());

        final ConfigOp config = geogit.command(ConfigOp.class);
        config.setAction(ConfigAction.CONFIG_UNSET).setName("remote." + remoteName + ".fetch")
                .call();

        final RemoteListOp remoteList = geogit.command(RemoteListOp.class);

        ImmutableList<Remote> allRemotes = remoteList.call();

        assertTrue(allRemotes.isEmpty());
    }
}
