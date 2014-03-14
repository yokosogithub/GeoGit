/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.RevFeature;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.GeogitCLI;
import org.geogit.cli.test.functional.CLITestInjectorBuilder;
import org.geogit.osm.internal.OSMImportOp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class OSMApplyDiffTest extends Assert {

    private GeogitCLI cli;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        File workingDirectory = tempFolder.getRoot();
        TestPlatform platform = new TestPlatform(workingDirectory);
        GlobalInjectorBuilder.builder = new CLITestInjectorBuilder(platform);
        cli.setPlatform(platform);
        cli.execute("init");
        cli.execute("config", "user.name", "Gabriel Roldan");
        cli.execute("config", "user.email", "groldan@opengeo.org");
        assertTrue(new File(workingDirectory, ".geogit").exists());

    }

    @Test
    public void testApplyDiff() throws Exception {
        // import and check
        GeoGIT geogit = cli.newGeoGIT();
        String filename = OSMImportOp.class.getResource("nodes_for_changeset2.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());

        long unstaged = geogit.getRepository().getWorkingTree().countUnstaged("node").getCount();
        assertTrue(unstaged > 0);
        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/2059114068").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("WORK_HEAD:node/507464865")
                .call(RevFeature.class);
        assertFalse(revFeature.isPresent());

        String changesetFilename = OSMImportOp.class.getResource("changeset.xml").getFile();
        File changesetFile = new File(changesetFilename);
        cli.execute("osm", "apply-diff", changesetFile.getAbsolutePath());

        revFeature = geogit.command(RevObjectParse.class).setRefSpec("WORK_HEAD:node/2059114068")
                .call(RevFeature.class);
        assertFalse(revFeature.isPresent());
        revFeature = geogit.command(RevObjectParse.class).setRefSpec("WORK_HEAD:node/507464865")
                .call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        geogit.close();
    }

}
