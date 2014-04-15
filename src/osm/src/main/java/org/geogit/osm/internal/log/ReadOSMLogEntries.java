/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal.log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.google.inject.Inject;

/**
 * Returns the set of entries of the OSM log in the current repository.
 */
public class ReadOSMLogEntries extends AbstractGeoGitOp<List<OSMLogEntry>> {

    private Platform platform;

    private OSMLogEntry entry;

    /**
     * Constructs a new instance of {@code ResolveOSMLogfile} with the specified platform.
     * 
     */
    @Inject
    public ReadOSMLogEntries() {

    }

    public ReadOSMLogEntries setEntry(OSMLogEntry entry) {
        this.entry = entry;
        return this;
    }

    @Override
    public List<OSMLogEntry> call() {
        URL url = command(ResolveOSMLogfile.class).call();
        File file = new File(url.getFile());
        List<OSMLogEntry> entries;
        try {
            synchronized (file.getCanonicalPath().intern()) {
                entries = Files.readLines(file, Charsets.UTF_8,
                        new LineProcessor<List<OSMLogEntry>>() {
                            List<OSMLogEntry> entries = Lists.newArrayList();

                            @Override
                            public List<OSMLogEntry> getResult() {
                                return entries;
                            }

                            @Override
                            public boolean processLine(String s) throws IOException {
                                OSMLogEntry entry = OSMLogEntry.valueOf(s);
                                entries.add(entry);
                                return true;
                            }
                        });
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return entries;
    }

}
