/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.osm.history.cli;

import java.io.File;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.apache.commons.io.FileUtils;
import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.cli.GeogitCLI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class OSMHistoryImportTest extends Assert {

    private GeogitCLI cli;

    private String fakeOsmApiUrl;

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);
        fakeOsmApiUrl = getClass().getResource("../internal/01_10").toExternalForm();

        File workingDirectory = new File("target", "repo");
        FileUtils.deleteDirectory(workingDirectory);
        assertTrue(workingDirectory.mkdir());
        Platform platform = new TestPlatform(workingDirectory);
        cli.setPlatform(platform);
        cli.execute("init");
        assertTrue(new File(workingDirectory, ".geogit").exists());
    }

    @Test
    public void test() throws Exception {
        cli.execute("config", "user.name", "Gabriel Roldan");
        cli.execute("config", "user.email", "groldan@opengeo.org");
        cli.execute("osm", "import-history", fakeOsmApiUrl, "--to", "9");
    }

}
