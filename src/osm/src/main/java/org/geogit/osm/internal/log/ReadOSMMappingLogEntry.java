/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal.log;

import java.io.File;
import java.io.IOException;

import org.geogit.api.AbstractGeoGitOp;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Returns the stored information about a mapping for a given tree path. Returns an absent object if
 * there is no information for the specified folder
 */
public class ReadOSMMappingLogEntry extends AbstractGeoGitOp<Optional<OSMMappingLogEntry>> {

    private String path;

    /**
     * Constructs a new instance of {@code ResolveOSMLogfile} with the specified platform.
     * 
     */
    @Inject
    public ReadOSMMappingLogEntry() {

    }

    public ReadOSMMappingLogEntry setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public Optional<OSMMappingLogEntry> call() {
        final File osmMapFolder = command(ResolveOSMMappingLogFolder.class).call();
        File file = new File(osmMapFolder, path);
        OSMMappingLogEntry entry = null;
        if (file.exists()) {
            try {
                synchronized (file.getCanonicalPath().intern()) {
                    String line = Files.readFirstLine(file, Charsets.UTF_8);
                    entry = OSMMappingLogEntry.fromString(line);
                }
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return Optional.fromNullable(entry);
    }
}
