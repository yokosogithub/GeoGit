/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal.log;

import java.io.File;
import java.io.IOException;

import jline.internal.Preconditions;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.osm.internal.Mapping;
import org.geogit.osm.internal.MappingRule;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Writes the mapping files that store the information about a mapping operation, storing the id
 * from which the affected trees have been mapped and the mapping code used
 */
public class WriteOSMMappingEntries extends AbstractGeoGitOp<Void> {

    private Mapping mapping;

    private OSMMappingLogEntry entry;

    @Inject
    public WriteOSMMappingEntries() {

    }

    public WriteOSMMappingEntries setMappingLogEntry(OSMMappingLogEntry entry) {
        this.entry = entry;
        return this;
    }

    public WriteOSMMappingEntries setMapping(Mapping mapping) {
        this.mapping = mapping;
        return this;
    }

    @Override
    public Void call() {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(mapping);
        final File osmMapFolder = command(ResolveOSMMappingLogFolder.class).call();
        for (MappingRule rule : mapping.getRules()) {
            File file = new File(osmMapFolder, rule.getName());
            try {
                Files.write(entry.toString(), file, Charsets.UTF_8);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        File file = new File(osmMapFolder, entry.getPostMappingId().toString());
        try {
            Files.write(mapping.toString(), file, Charsets.UTF_8);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return null;
    }
}
