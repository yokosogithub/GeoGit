/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import org.geogit.api.Platform;
import org.geogit.storage.BlueprintsGraphDatabase;

import com.google.inject.Inject;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * Provides an implementation of a GeoGit Graph Database using TinkerGraph's {@link TinkerGraph
 * in-memory only database implementation}.
 */
public class HeapGraphDatabase extends BlueprintsGraphDatabase<TinkerGraph> {
    /**
     * Constructs a new {@code TinkerGraphDatabase} using the given platform.
     * 
     * @param platform the platform to use.
     */
    @Inject
    public HeapGraphDatabase(final Platform platform) {
        super(platform);
    }

    @Override
    protected TinkerGraph getGraphDatabase() {
        return new TinkerGraph();
    }

    @Override
    public void configure() {
        // No-op
    }
}
