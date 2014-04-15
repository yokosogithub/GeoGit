/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.diff;

import org.geogit.storage.FieldType;
import org.geogit.storage.text.TextValueSerializer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Geometry;

/**
 * An implementation of GeometryDiff that just stores both the new and the old value, so it actually
 * has the least compact representation, but can be used in all cases.
 * 
 * 
 */
public class DefaultGeometryDiffImpl {

    private Optional<Geometry> oldGeom;

    private Optional<Geometry> newGeom;

    public DefaultGeometryDiffImpl(Optional<Geometry> oldGeom, Optional<Geometry> newGeom) {
        this.oldGeom = oldGeom;
        this.newGeom = newGeom;
    }

    public DefaultGeometryDiffImpl(String s) {
        String[] tokens = s.split("\t");
        Preconditions.checkArgument(tokens.length == 2, "Wrong difference definition:", s);
        oldGeom = Optional.fromNullable((Geometry) TextValueSerializer.fromString(
                FieldType.forBinding(Geometry.class), tokens[0]));
        newGeom = Optional.fromNullable((Geometry) TextValueSerializer.fromString(
                FieldType.forBinding(Geometry.class), tokens[1]));

    }

    private CharSequence geometryValueAsString(Optional<Geometry> value) {
        return TextValueSerializer.asString(Optional.fromNullable((Object) value.orNull()));
    }

    public DefaultGeometryDiffImpl reversed() {
        return new DefaultGeometryDiffImpl(newGeom, oldGeom);
    }

    public boolean canBeAppliedOn(Optional<Geometry> obj) {
        return obj.equals(oldGeom);
    }

    public Optional<Geometry> applyOn(Optional<Geometry> obj) {
        Preconditions.checkArgument(canBeAppliedOn(obj));
        return newGeom;
    }

    public String toString() {
        return geometryValueAsString(oldGeom) + " -> " + geometryValueAsString(newGeom);
    }

    public String asText() {
        return geometryValueAsString(oldGeom) + "\t" + geometryValueAsString(newGeom);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DefaultGeometryDiffImpl)) {
            return false;
        }
        DefaultGeometryDiffImpl d = (DefaultGeometryDiffImpl) o;
        return d.oldGeom.equals(oldGeom) && d.newGeom.equals(newGeom);
    }

}
