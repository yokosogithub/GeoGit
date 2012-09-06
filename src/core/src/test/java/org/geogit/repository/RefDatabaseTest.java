/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.RefDatabase;
import org.geogit.test.RepositoryTestCase;
import org.junit.Test;

public class RefDatabaseTest extends RepositoryTestCase {

    private RefDatabase refDb;

    @Override
    protected void setUpInternal() throws Exception {
        refDb = repo.getRefDatabase();
    }

    @Test
    public void testEmpty() {
        assertEquals(ObjectId.NULL, refDb.getRef(Ref.MASTER).getObjectId());
        assertEquals(ObjectId.NULL, refDb.getRef(Ref.HEAD).getObjectId());
    }

    @Test
    public void testPutGetRef() {
        byte[] raw = new byte[20];
        Arrays.fill(raw, (byte) 1);
        ObjectId oid = new ObjectId(raw);
        Ref ref = new Ref("refs/HEAD", oid, TYPE.COMMIT);
        assertTrue(refDb.put(ref));
        assertFalse(refDb.put(ref));

        Ref read = refDb.getRef("refs/HEAD");
        assertEquals(ref, read);
    }

}
