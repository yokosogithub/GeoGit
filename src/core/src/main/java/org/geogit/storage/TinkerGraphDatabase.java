/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage;

import org.geogit.api.Platform;

import com.google.inject.Inject;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * Provides an implementation of a GeoGit Graph Database using TinkerGraph.
 */
public class TinkerGraphDatabase extends BlueprintsGraphDatabase<TinkerGraph> {
    /**
     * Constructs a new {@code TinkerGraphDatabase} using the given platform.
     * 
     * @param platform the platform to use.
     */
    @Inject
    public TinkerGraphDatabase(final Platform platform) {
        super(platform);
    }

    @Override
    protected TinkerGraph getGraphDatabase() {
        return new TinkerGraph(dbPath, TinkerGraph.FileType.GML);
    }
}
