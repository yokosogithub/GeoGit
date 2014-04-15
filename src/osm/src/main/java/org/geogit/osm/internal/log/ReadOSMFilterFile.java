/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal.log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import jline.internal.Preconditions;

import org.geogit.api.AbstractGeoGitOp;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Reads the file filter associated to a previously executed OSM import operation.
 */
public class ReadOSMFilterFile extends AbstractGeoGitOp<Optional<String>> {

    private OSMLogEntry entry;

    @Inject
    public ReadOSMFilterFile() {

    }

    public ReadOSMFilterFile setEntry(OSMLogEntry entry) {
        this.entry = entry;
        return this;
    }

    @Override
    public Optional<String> call() {
        Preconditions.checkNotNull(entry);
        URL url = command(ResolveOSMLogfile.class).call();
        File logfile = new File(url.getFile());
        File file = new File(logfile.getParentFile(), "filter" + entry.getId().toString());
        if (!file.exists()) {
            return Optional.absent();
        }
        try {
            List<String> lines = Files.readLines(file, Charsets.UTF_8);
            String line = Joiner.on("\n").join(lines);
            return Optional.of(line);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

}
