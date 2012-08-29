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
package org.geogit.cli.porcelain;

import java.util.List;

import org.geogit.api.RevCommit;
import org.geogit.cli.CLITest;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * @author groldan
 * 
 */
public class LogTest extends CLITest {

    @Test
    public void test() throws Exception {
        cli.execute("log");

        List<String> cmdOutput = super.parseOutput(false);

        assertEquals(4/* commits */* 4 /* lines/commit */, cmdOutput.size());

        for (RevCommit c : ImmutableList.of(commit4, commit3, commit2, commit1)) {
            String commitLine = cmdOutput.remove(0);
            String authorLine = cmdOutput.remove(0);
            String dateLine = cmdOutput.remove(0);
            String messageLine = cmdOutput.remove(0);

            String expected = "Commit:" + c.getId().toString();
            String actual = commitLine.replaceAll(" ", "");
            assertEquals(expected, actual);

        }
    }

}
