package org.geogit.api.plumbing.diff;

import org.geogit.storage.text.AttributeValueSerializer;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Geometry;

/**
 * An implementation of AttributeDiff to be used with attributes containing geometries
 * 
 */
public class GeometryAttributeDiff implements AttributeDiff {

    private TYPE type;

    private Optional<Geometry> geometry;

    private LCSGeometryDiffImpl diff;

    public GeometryAttributeDiff(Optional<Geometry> oldGeom, Optional<Geometry> newGeom) {
        Preconditions.checkArgument(oldGeom != null || newGeom != null);
        if (newGeom == null) {
            type = TYPE.REMOVED;
            geometry = oldGeom;
        } else if (oldGeom == null) {
            type = TYPE.ADDED;
            geometry = newGeom;
        } else {
            type = TYPE.MODIFIED;
            diff = new LCSGeometryDiffImpl(oldGeom, newGeom);
        }

    }

    public GeometryAttributeDiff(LCSGeometryDiffImpl diff) {
        type = TYPE.MODIFIED;
        this.diff = diff;
    }

    public GeometryAttributeDiff(String s) {
        String[] tokens = s.split("\t");
        if (tokens[0].equals("M")) {
            type = TYPE.MODIFIED;
            diff = new LCSGeometryDiffImpl(s.substring(s.indexOf("\t") + 1));
        } else if (tokens[0].equals("A")) {
            Preconditions.checkArgument(tokens.length == 3);
            type = TYPE.ADDED;
            geometry = Optional.fromNullable((Geometry) AttributeValueSerializer.fromText(
                    Geometry.class.getName(), tokens[1]));
        } else if (tokens[0].equals("R")) {
            Preconditions.checkArgument(tokens.length == 3);
            type = TYPE.REMOVED;
            geometry = Optional.fromNullable((Geometry) AttributeValueSerializer.fromText(
                    Geometry.class.getName(), tokens[1]));
        } else {
            throw new IllegalArgumentException("Wrong difference definition:" + s);
        }

    }

    @Override
    public TYPE getType() {
        return type;
    }

    @Override
    public AttributeDiff reversed() {
        if (type == TYPE.MODIFIED) {
            return new GeometryAttributeDiff(this.diff.reversed());
        } else if (type == TYPE.REMOVED) {
            return new GeometryAttributeDiff(null, geometry);
        } else {
            return new GeometryAttributeDiff(geometry, null);
        }
    }

    @Override
    public Optional<?> applyOn(Optional<?> obj) {
        Preconditions.checkState(canBeAppliedOn(obj));
        switch (type) {
        case ADDED:
            return geometry;
        case REMOVED:
            return null;
        case MODIFIED:
        default:
            return diff.applyOn((Optional<Geometry>) obj);
        }
    }

    @Override
    public boolean canBeAppliedOn(Optional<?> obj) {
        switch (type) {
        case ADDED:
            return obj == null;
        case REMOVED:
            return obj.equals(geometry);
        case MODIFIED:
        default:
            return diff.canBeAppliedOn((Optional<Geometry>) obj);
        }

    }

    public String toString() {
        switch (type) {
        case ADDED:
            return "[MISSING] -> " + AttributeValueSerializer.asText(geometry);
        case REMOVED:
            return AttributeValueSerializer.asText(geometry) + " -> [MISSING]";
        case MODIFIED:
        default:
            return diff.toString();
        }
    }

    @Override
    public String asText() {
        switch (type) {
        case ADDED:
        case REMOVED:
            return type.name().toCharArray()[0] + "\t" + AttributeValueSerializer.asText(geometry);
        case MODIFIED:
        default:
            return type.name().toCharArray()[0] + "\t" + diff.asText();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeometryAttributeDiff)) {
            return false;
        }
        GeometryAttributeDiff d = (GeometryAttributeDiff) o;
        if (geometry != null) {
            return geometry.equals(d.geometry) && type == d.type;
        } else {
            return diff.equals(d.diff);
        }
    }

    /**
     * Returns the difference corresponding to the case of a modified attributed. If the attribute
     * is of type ADDED or REMOVED, this method will return null
     */
    public LCSGeometryDiffImpl getDiff() {
        return diff;
    }

}
