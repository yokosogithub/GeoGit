/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal;

import java.io.File;

import org.geogit.api.RevFeature;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class OSMAplyDiffOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testApplyChangeset() throws Exception {
        String filename = getClass().getResource("nodes_for_changeset2.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();
        long unstaged = geogit.getRepository().getWorkingTree().countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/2059114068").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("WORK_HEAD:node/507464865")
                .call(RevFeature.class);
        assertFalse(revFeature.isPresent());

        String changesetFilename = getClass().getResource("changeset.xml").getFile();
        geogit.command(OSMApplyDiffOp.class).setDiffFile(new File(changesetFilename)).call();
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("WORK_HEAD:node/2059114068")
                .call(RevFeature.class);
        assertFalse(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("WORK_HEAD:node/507464865")
                .call(RevFeature.class);
        assertTrue(revFeature.isPresent());

    }
}
