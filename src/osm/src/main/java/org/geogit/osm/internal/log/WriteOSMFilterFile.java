/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal.log;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jline.internal.Preconditions;

import org.geogit.api.AbstractGeoGitOp;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Writes the file filter associated to an OSM import operation.
 */
public class WriteOSMFilterFile extends AbstractGeoGitOp<Void> {

    private OSMLogEntry entry;

    private String filter;

    @Inject
    public WriteOSMFilterFile() {

    }

    public WriteOSMFilterFile setEntry(OSMLogEntry entry) {
        this.entry = entry;
        return this;
    }

    public WriteOSMFilterFile setFilterCode(String filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public Void call() {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(filter);
        URL url = command(ResolveOSMLogfile.class).call();
        File logfile = new File(url.getFile());
        File file = new File(logfile.getParentFile(), "filter" + entry.getId().toString());
        try {
            Files.write(filter, file, Charsets.UTF_8);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return null;
    }

}
