/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.repository;

import javax.annotation.Nullable;

import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.RevFeature;
import org.geogit.api.RevTree;
import org.geotools.geometry.jts.JTS;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Utility methods to deal with various spatial operations
 * 
 */
public class SpatialOps {

    private static final GeometryFactory gfac = new GeometryFactory();

    /**
     * @param oldObject
     * @param newObject
     * @return the aggregated bounding box
     */
    public static com.vividsolutions.jts.geom.Envelope aggregatedBounds(Node oldObject,
            Node newObject) {
        Envelope env = new Envelope();
        if (oldObject != null) {
            oldObject.expand(env);
        }
        if (newObject != null) {
            newObject.expand(env);
        }
        return env;
    }

    /**
     * Creates and returns a geometry out of bounds (a point if bounds.getSpan(0) ==
     * bounds.getSpan(1) == 0D, a polygon otherwise), setting the bounds
     * {@link BoundingBox#getCoordinateReferenceSystem() CRS} as the geometry's
     * {@link Geometry#getUserData() user data}.
     * 
     * @param bounds the bounding box to build from
     * @return the newly constructed geometry
     */
    public static Geometry toGeometry(final BoundingBox bounds) {
        if (bounds == null) {
            return null;
        }
        Geometry geom;
        if (bounds.getSpan(0) == 0D && bounds.getSpan(1) == 0D) {
            geom = gfac.createPoint(new Coordinate(bounds.getMinX(), bounds.getMinY()));
        } else {
            geom = JTS.toGeometry(bounds, gfac);
        }
        geom.setUserData(bounds.getCoordinateReferenceSystem());
        return geom;
    }

    public static Envelope boundsOf(RevTree tree) {
        Envelope env = new Envelope();
        if (tree.buckets().isPresent()) {
            for (Bucket bucket : tree.buckets().get().values()) {
                bucket.expand(env);
            }
        } else {
            if (tree.trees().isPresent()) {
                ImmutableList<Node> trees = tree.trees().get();
                for (int i = 0; i < trees.size(); i++) {
                    trees.get(i).expand(env);
                }
            }
            if (tree.features().isPresent()) {
                ImmutableList<Node> trees = tree.features().get();
                for (int i = 0; i < trees.size(); i++) {
                    trees.get(i).expand(env);
                }
            }
        }
        return env;
    }

    @Nullable
    public static Envelope boundsOf(RevFeature feat) {
        Envelope env = null;
        for (Optional<Object> opt : feat.getValues()) {
            if (opt.isPresent() && opt.get() instanceof Geometry) {
                if (env == null) {
                    env = new Envelope();
                }
                env.expandToInclude(((Geometry) opt.get()).getEnvelopeInternal());
            }
        }
        return env;
    }
}
