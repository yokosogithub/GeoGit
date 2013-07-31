/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.internal.CreateOSMChangesetOpTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CreateOSMChangesetTest extends Assert {

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
        String filename = CreateOSMChangesetOpTest.class.getResource("nodes_for_changeset.xml")
                .getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "message");
        filename = CreateOSMChangesetOpTest.class.getResource("nodes_for_changeset2.xml").getFile();
        file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("add");
        cli.execute("commit", "-m", "message2");
        File changesetFile = new File(tempFolder.getRoot(), "changesets.xml");
        cli.execute("osm", "create-changeset", "HEAD", "HEAD~1", "-f",
                changesetFile.getAbsolutePath());
        assertTrue(changesetFile.exists());
        String expectedFile = CreateOSMChangesetOpTest.class.getResource("changesets.xml")
                .getFile();
        assertTrue(compareFiles(expectedFile, changesetFile));

    }

    private boolean compareFiles(String expectedFile, File changesetFile) throws Exception {

        FileInputStream expectedFin = new FileInputStream(expectedFile);
        BufferedReader expected = new BufferedReader(new InputStreamReader(expectedFin));
        FileInputStream changesetFin = new FileInputStream(changesetFile);
        BufferedReader changeset = new BufferedReader(new InputStreamReader(changesetFin));
        String expectedLine;
        String changesetLine;
        while ((expectedLine = expected.readLine()) != null
                && (changesetLine = changeset.readLine()) != null) {
            if (!changesetLine.equals(expectedLine)) {
                expected.close();
                expectedFin.close();
                changeset.close();
                changesetFin.close();
                return false;
            }
        }
        expected.close();
        expectedFin.close();
        changeset.close();
        changesetFin.close();
        return true;

    }

    // @Test
    // public void testCreateChangesetsWithoutCommitting() throws Exception {
    // String filename = CreateOSMChangesetOpTest.class.getResource("nodes.xml").getFile();
    // File file = new File(filename);
    // cli.execute("osm", "import", file.getAbsolutePath());
    // File changesetFile = new File(tempFolder.getRoot(), "changesets.xml");
    // cli.execute("osm", "create-changeset", "-f",
    // changesetFile.getAbsolutePath());
    // assertTrue(changesetFile.exists());
    // assertTrue(changesetFile.length() > 0);// TODO??
    //
    // }
}
