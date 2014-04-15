/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal.log;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jline.internal.Preconditions;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.osm.internal.Mapping;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Reads the mapping associated to a previously executed OSM mapping operation.
 */
public class ReadOSMMapping extends AbstractGeoGitOp<Optional<Mapping>> {

    private OSMMappingLogEntry entry;

    @Inject
    public ReadOSMMapping() {

    }

    public ReadOSMMapping setEntry(OSMMappingLogEntry entry) {
        this.entry = entry;
        return this;
    }

    @Override
    public Optional<Mapping> call() {
        Preconditions.checkNotNull(entry);
        final File osmMapFolder = command(ResolveOSMMappingLogFolder.class).call();
        File file = new File(osmMapFolder, entry.getPostMappingId().toString());
        if (!file.exists()) {
            return Optional.absent();
        }
        try {
            List<String> lines = Files.readLines(file, Charsets.UTF_8);
            String s = Joiner.on("\n").join(lines);
            Mapping mapping = Mapping.fromString(s);
            return Optional.of(mapping);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

}
