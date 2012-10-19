/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.geogit.api.CommandLocator;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.StagingDatabase;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class RevParseTest {

    private CommandLocator mockCommands;

    private RevParse command;

    private StagingDatabase mockIndexDb;

    private RefParse mockRefParse;

    @Before
    public void setUp() {
        mockCommands = mock(CommandLocator.class);
        mockIndexDb = mock(StagingDatabase.class);
        command = new RevParse(mockIndexDb);
        command.setCommandLocator(mockCommands);

        mockRefParse = mock(RefParse.class);
        when(mockRefParse.setName(anyString())).thenReturn(mockRefParse);
        when(mockCommands.command(eq(RefParse.class))).thenReturn(mockRefParse);
    }

    @Test
    public void testParseRef() {

        ObjectId oid = ObjectId.forString("hash me out");
        Optional<Ref> ref = Optional.of(new Ref(Ref.MASTER, oid, TYPE.COMMIT));
        when(mockRefParse.call()).thenReturn(ref);

        Optional<ObjectId> objectId = command.setRefSpec(Ref.MASTER).call();
        assertEquals(oid, objectId.get());
    }

    @Test
    public void testResolveToNothing() throws Exception {

        Optional<Ref> ref = Optional.absent();
        when(mockRefParse.call()).thenReturn(ref);
        assertFalse(command.setRefSpec("abcNotAHash").call().isPresent());

        assertFalse(command.setRefSpec("refs/norARef").call().isPresent());

        String validHashButNonExistent = ObjectId.forString("hash me").toString();
        ImmutableList<ObjectId> empty = ImmutableList.of();
        when(mockIndexDb.lookUp(eq(validHashButNonExistent))).thenReturn(empty);

        assertFalse(command.setRefSpec(validHashButNonExistent).call().isPresent());

        verify(mockIndexDb, atLeast(1)).lookUp(eq(validHashButNonExistent));
    }

    @Test
    public void testResolveObjectId() throws Exception {
        byte[] raw1 = { 'a', 'b', 'c', 'd', 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        byte[] raw2 = { 'a', 'b', 'c', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        Optional<Ref> ref = Optional.absent();
        when(mockRefParse.call()).thenReturn(ref);

        ObjectId id1 = new ObjectId(raw1);
        ObjectId id2 = new ObjectId(raw2);
        String hash1 = id1.toString();
        String hash2 = id2.toString();

        when(mockIndexDb.lookUp(eq(hash1))).thenReturn(ImmutableList.of(id1));
        when(mockIndexDb.lookUp(eq(hash2))).thenReturn(ImmutableList.of(id2));

        ObjectId objectId;
        objectId = command.setRefSpec(hash1).call().get();
        assertEquals(id1, objectId);

        objectId = command.setRefSpec(hash2).call().get();
        assertEquals(id2, objectId);
    }

    @Test
    public void testResolveMultiple() throws Exception {
        byte[] raw1 = { 'a', 'b', 'c', 'd', 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        byte[] raw2 = { 'a', 'b', 'c', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        Optional<Ref> ref = Optional.absent();
        when(mockRefParse.call()).thenReturn(ref);

        ObjectId id1 = new ObjectId(raw1);
        ObjectId id2 = new ObjectId(raw2);
        String hash1 = id1.toString();
        String hash2 = id2.toString();
        //
        // repo.getObjectDatabase().put(id1, new BlobWriter(new byte[10]));
        // repo.getObjectDatabase().put(id2, new BlobWriter(new byte[10]));
        //
        String prefixSearch;
        for (int i = 1; i <= 3; i++) {
            prefixSearch = hash1.substring(0, 2 * i);
            when(mockIndexDb.lookUp(eq(prefixSearch))).thenReturn(ImmutableList.of(id1, id2));
            try {
                command.setRefSpec(prefixSearch).call();
                fail("Expected IAE on multiple results");
            } catch (IllegalArgumentException e) {
                assertTrue(true);
            }
        }

        prefixSearch = hash1.substring(0, 7);
        when(mockIndexDb.lookUp(eq(prefixSearch))).thenReturn(ImmutableList.of(id1));
        ObjectId objectId = command.setRefSpec(prefixSearch).call().get();
        assertEquals(id1, objectId);

        prefixSearch = hash2.substring(0, 7);
        when(mockIndexDb.lookUp(eq(prefixSearch))).thenReturn(ImmutableList.of(id2));
        objectId = command.setRefSpec(prefixSearch).call().get();
        assertEquals(id2, objectId);
    }

}
