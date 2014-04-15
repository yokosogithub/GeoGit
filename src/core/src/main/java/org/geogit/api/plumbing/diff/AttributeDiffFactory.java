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

public class AttributeDiffFactory {

    public static AttributeDiff attributeDiffFromText(Class<?> clazz, String s) {

        String[] tokens = s.split("\t");
        AttributeDiff ad;
        if (Geometry.class.isAssignableFrom(clazz)) {
            ad = new GeometryAttributeDiff(s);
        } else {
            if (AttributeDiff.TYPE.REMOVED.name().startsWith(tokens[0])) {
                Preconditions.checkArgument(tokens.length == 2, "Wrong difference definition:", s);
                Object oldValue = TextValueSerializer.fromString(FieldType.forBinding(clazz),
                        tokens[1]);
                ad = new GenericAttributeDiffImpl(Optional.fromNullable(oldValue), null);
            } else if (AttributeDiff.TYPE.ADDED.name().startsWith(tokens[0])) {
                Preconditions.checkArgument(tokens.length == 2, "Wrong difference definition:", s);
                Object newValue = TextValueSerializer.fromString(FieldType.forBinding(clazz),
                        tokens[1]);
                ad = new GenericAttributeDiffImpl(null, Optional.fromNullable(newValue));
            } else if (AttributeDiff.TYPE.MODIFIED.name().startsWith(tokens[0])) {
                Preconditions.checkArgument(tokens.length == 3, "Wrong difference definition:", s);
                Object oldValue = TextValueSerializer.fromString(FieldType.forBinding(clazz),
                        tokens[1]);
                Object newValue = TextValueSerializer.fromString(FieldType.forBinding(clazz),
                        tokens[2]);
                ad = new GenericAttributeDiffImpl(Optional.fromNullable(oldValue),
                        Optional.fromNullable(newValue));
            } else {
                throw new IllegalArgumentException("Wrong difference definition:" + s);
            }
        }
        return ad;

    }

}
