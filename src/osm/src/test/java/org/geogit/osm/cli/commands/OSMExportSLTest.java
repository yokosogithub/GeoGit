/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli.commands;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.cli.commands.OSMMap;
import org.geogit.osm.internal.OSMImportOp;
import org.geogit.repository.WorkingTree;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sqlite.SQLiteConfig;

public class OSMExportSLTest extends Assert {

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


        // Use in-memory database to test whether we can load Spatialite extension
        Connection connection = null;
        Throwable thrown = null;

        try {
            Class.forName("org.sqlite.JDBC");
            SQLiteConfig config = new SQLiteConfig();
            config.enableSpatiaLite(true);
            connection = DriverManager.getConnection("jdbc:sqlite::memory:", config.toProperties());
            Statement statement = connection.createStatement();
            statement.execute("SELECT InitSpatialMetaData();");
        } catch (SQLException e) {
            thrown = e;
            thrown.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        if (thrown != null) {
            thrown.printStackTrace();
            Assume.assumeNoException(thrown);
        }
    }

    @Test
    public void testExportWithMapping() throws Exception {
        String filename = OSMImportOp.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        WorkingTree workTree = cli.getGeogit().getRepository().getWorkingTree();
        long unstaged = workTree.countUnstaged("way");
        assertTrue(unstaged > 0);
        unstaged = workTree.countUnstaged("node");
        assertTrue(unstaged > 0);
        String mappingFilename = OSMMap.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        File exportFile = new File(tempFolder.getRoot(), "export.sqlite");
        cli.execute("osm", "export-sl", "--database", exportFile.getAbsolutePath(), "--mapping",
                mappingFile.getAbsolutePath());
        assertTrue(exportFile.exists());
        cli.execute("sl", "import", "-t", "onewaystreets", "--database",
                exportFile.getAbsolutePath());
        unstaged = workTree.countUnstaged("onewaystreets");
        assertTrue(unstaged > 0);
    }

}
