/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.cli.test.functional;

import static org.geogit.cli.test.functional.GlobalState.currentDirectory;
import static org.geogit.cli.test.functional.GlobalState.stdIn;
import static org.geogit.cli.test.functional.GlobalState.stdOut;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.Platform;
import org.geogit.cli.GeogitCLI;
import org.geotools.util.logging.Logging;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

public abstract class AbstractGeogitFunctionalTest {

    static {
        Logging.ALL.forceMonolineConsoleOutput(java.util.logging.Level.SEVERE);
    }

    /**
     * Runs the given command with its arguments and returns the command output as a list of
     * strings, one per line.
     */
    protected List<String> runAndParseCommand(String... command) throws Exception {

        ByteArrayOutputStream out = runCommand(command);
        InputSupplier<InputStreamReader> readerSupplier = CharStreams.newReaderSupplier(
                ByteStreams.newInputStreamSupplier(out.toByteArray()), Charset.forName("UTF-8"));
        List<String> lines = CharStreams.readLines(readerSupplier);
        return lines;
    }

    protected ByteArrayOutputStream runCommand(String... command) throws Exception {
        // System.err.println("Running command " + Arrays.toString(command));
        assertNotNull(currentDirectory);

        stdIn = new ByteArrayInputStream(new byte[0]);
        stdOut = new ByteArrayOutputStream();

        Platform platform = new TestPlatform(currentDirectory);

        ConsoleReader consoleReader = new ConsoleReader(stdIn, stdOut, new UnsupportedTerminal());

        GeoGIT geogit = new GeoGIT(currentDirectory);
        try {
            GeogitCLI cli = new GeogitCLI(consoleReader);
            cli.setGeogit(geogit);
            cli.setPlatform(platform);
            cli.execute(command);
        } finally {
            geogit.close();
        }
        return stdOut;
    }
}
