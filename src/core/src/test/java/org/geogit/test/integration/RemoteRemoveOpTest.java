/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.RemoteAddOp;
import org.geogit.api.porcelain.RemoteException;
import org.geogit.api.porcelain.RemoteRemoveOp;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class RemoteRemoveOpTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public final void setUpInternal() {
    }

    @Test
    public void testNullName() {
        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        exception.expect(RemoteException.class);
        remoteRemove.setName(null).call();
    }

    @Test
    public void testEmptyName() {
        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        exception.expect(RemoteException.class);
        remoteRemove.setName("").call();
    }

    @Test
    public void testRemoveNoRemotes() {
        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        exception.expect(RemoteException.class);
        remoteRemove.setName("remote").call();
    }

    @Test
    public void testRemoveNonexistentRemote() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", remote.getFetch());

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        exception.expect(RemoteException.class);
        remoteRemove.setName("nonexistent").call();
    }

    @Test
    public void testRemoveRemote() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", remote.getFetch());

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(remoteName, deletedRemote.getName());
        assertEquals(remoteURL, deletedRemote.getFetchURL());
        assertEquals(remoteURL, deletedRemote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", deletedRemote.getFetch());
    }

    @Test
    public void testRemoveRemoteWithRefs() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", remote.getFetch());

        String refName = Ref.REMOTES_PREFIX + remoteName + "/branch1";
        geogit.command(UpdateRef.class).setName(refName).setNewValue(ObjectId.NULL).call();

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        Optional<Ref> remoteRef = geogit.command(RefParse.class).setName(refName).call();

        assertFalse(remoteRef.isPresent());

        assertEquals(remoteName, deletedRemote.getName());
        assertEquals(remoteURL, deletedRemote.getFetchURL());
        assertEquals(remoteURL, deletedRemote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", deletedRemote.getFetch());
    }

    @Test
    public void testRemoveRemoteWithNoURL() {
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

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(remoteName, deletedRemote.getName());
        assertEquals("", deletedRemote.getFetchURL());
        assertEquals("", deletedRemote.getPushURL());
        assertEquals("+refs/heads/*:refs/remotes/" + remoteName + "/*", deletedRemote.getFetch());
    }

    @Test
    public void testRemoveRemoteWithNoFetch() {
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

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(remoteName, deletedRemote.getName());
        assertEquals(remoteURL, deletedRemote.getFetchURL());
        assertEquals(remoteURL, deletedRemote.getPushURL());
        assertEquals("", deletedRemote.getFetch());
    }
}
