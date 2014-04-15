/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.internal;

import java.util.List;

import javax.annotation.Nullable;

import com.vividsolutions.jts.geom.Coordinate;

interface PointCache {

    public abstract void put(Long nodeId, Coordinate coord);

    @Nullable
    public abstract Coordinate get(long nodeId);

    public void dispose();

    public Coordinate[] get(List<Long> ids);

}