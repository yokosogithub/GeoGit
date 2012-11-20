package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
    public void testRemoveNonexistantRemote() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        exception.expect(RemoteException.class);
        remoteRemove.setName("nonexistant").call();
    }

    @Test
    public void testRemoveRemote() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(deletedRemote.getName(), remoteName);
        assertEquals(deletedRemote.getFetchURL(), remoteURL);
        assertEquals(deletedRemote.getPushURL(), remoteURL);
        assertEquals(deletedRemote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");
    }

    @Test
    public void testRemoveRemoteWithRefs() {
        final RemoteAddOp remoteAdd = geogit.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remote.getName(), remoteName);
        assertEquals(remote.getFetchURL(), remoteURL);
        assertEquals(remote.getPushURL(), remoteURL);
        assertEquals(remote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");

        String refName = Ref.REMOTES_PREFIX + remoteName + "/branch1";
        geogit.command(UpdateRef.class).setName(refName).setNewValue(ObjectId.NULL).call();

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        Optional<Ref> remoteRef = geogit.command(RefParse.class).setName(refName).call();

        assertFalse(remoteRef.isPresent());

        assertEquals(deletedRemote.getName(), remoteName);
        assertEquals(deletedRemote.getFetchURL(), remoteURL);
        assertEquals(deletedRemote.getPushURL(), remoteURL);
        assertEquals(deletedRemote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");
    }

    @Test
    public void testRemoveRemoteWithNoURL() {
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

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(deletedRemote.getName(), remoteName);
        assertEquals(deletedRemote.getFetchURL(), "");
        assertEquals(deletedRemote.getPushURL(), "");
        assertEquals(deletedRemote.getFetch(), "+refs/heads/*:refs/remotes/" + remoteName + "/*");
    }

    @Test
    public void testRemoveRemoteWithNoFetch() {
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

        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        Remote deletedRemote = remoteRemove.setName(remoteName).call();

        assertEquals(deletedRemote.getName(), remoteName);
        assertEquals(deletedRemote.getFetchURL(), remoteURL);
        assertEquals(deletedRemote.getPushURL(), remoteURL);
        assertEquals(deletedRemote.getFetch(), "");
    }

    @Test
    public void testAccessorsAndMutators() {
        final RemoteRemoveOp remoteRemove = geogit.command(RemoteRemoveOp.class);

        String remoteName = "myremote";
        remoteRemove.setName(remoteName);
        assertEquals(remoteName, remoteRemove.getName());
    }
}
