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
package org.geogit.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.annotation.Nullable;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.RevCommit;
import org.geogit.test.RepositoryTestCase;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

/**
 * 
 */
public abstract class CLITest extends RepositoryTestCase {

    protected GeogitCLI cli;

    protected ByteArrayOutputStream stdOut = new ByteArrayOutputStream();

    protected RevCommit commit4;

    protected RevCommit commit3;

    protected RevCommit commit2;

    protected RevCommit commit1;

    /**
     * @throws Exception
     * @see org.geogit.test.RepositoryTestCase#setUpInternal()
     */
    @Override
    protected void setUpInternal() throws Exception {

        cli = new GeogitCLI(new ConsoleReader(System.in, stdOut, new UnsupportedTerminal()));
        cli.setPlatform(geogit.getPlatform());
        cli.setGeogit(geogit);

        insertAndAdd(lines1, lines2);
        commit1 = geogit.commit().setAuthor("groldan").setCommitter("juan").setMessage("commit 1")
                .call();

        insertAndAdd(points1, points2);
        commit2 = geogit.commit().setAuthor("juan").setCommitter("groldan").setMessage("commit 2")
                .call();

        insertAndAdd(points3, lines3);
        commit3 = geogit.commit().setAuthor("mike").setCommitter("groldan").setMessage("commit 3")
                .call();

        deleteAndAdd(points1);
        deleteAndAdd(lines1);
        commit4 = geogit.commit().setAuthor("mike").setCommitter("groldan").setMessage("commit 4")
                .call();
    }

    public List<String> parseOutput() {
        return parseOutput(true);
    }

    public List<String> parseOutput(boolean includeEmptyLines) {
        try {
            List<String> lines = CharStreams.readLines(new InputStreamReader(
                    new ByteArrayInputStream(stdOut.toByteArray())));
            if (!includeEmptyLines) {
                lines = Lists.newArrayList(Collections2.filter(lines, new Predicate<String>() {

                    @Override
                    public boolean apply(@Nullable String input) {
                        return !input.isEmpty();
                    }
                }));
            }
            return lines;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
