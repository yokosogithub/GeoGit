/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.changeset.internal;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.cli.GeogitCLI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;

import com.google.common.collect.Lists;

public class CreateOSMChangesetOpTest extends Assert {

    private GeogitCLI cli;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);
        File workingDirectory = tempFolder.getRoot();
        Platform platform = new TestPlatform(workingDirectory);
        cli.setPlatform(platform);
        cli.execute("init");
        cli.execute("config", "user.name", "Gabriel Roldan");
        cli.execute("config", "user.email", "groldan@opengeo.org");
        assertTrue(new File(workingDirectory, ".geogit").exists());
    }

    @Test
    public void testCreateChangesets() throws Exception {
        String filename = getClass().getResource("nodes.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        long unstaged = cli.getGeogit().getRepository().getWorkingTree().countUnstaged("node");
        assertTrue(unstaged > 0);
        cli.execute("add");
        cli.execute("commit", "-m", "commit1");
        filename = getClass().getResource("nodes2.xml").getFile();
        file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "commit1");
        Iterator<ChangeContainer> changes = cli.getGeogit().command(CreateOSMChangesetOp.class)
                .setNewVersion("HEAD").setOldVersion("HEAD~1").call();
        List<ChangeContainer> list = Lists.newArrayList(changes);
        assertFalse(list.isEmpty());
    }

}
