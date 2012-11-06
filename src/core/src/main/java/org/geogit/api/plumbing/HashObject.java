/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.MutableTree;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.api.SpatialRef;
import org.geogit.api.TreeVisitor;
import org.geogit.storage.GtEntityType;
import org.geotools.referencing.CRS;
import org.geotools.referencing.wkt.Formattable;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Hashes a RevObject and returns the ObjectId.
 * 
 * @author jgarrett
 * @see RevObject
 * @see ObjectId
 */
public class HashObject extends AbstractGeoGitOp<ObjectId> {

    @SuppressWarnings("unchecked")
    private static Map<CoordinateReferenceSystem, String> crsIdCache = Collections
            .synchronizedMap(new LRUMap(3));

    public enum TreeNode {
        REF(0), TREE(1), END(2);

        private int value;

        TreeNode(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private RevObject object;

    // This random byte code is used to represent null in hashing. This is intended to be something
    // that would be unlikely to duplicated by accident with real data. Changing this will cause all
    // objects that contain null values to hash differently.
    private final byte[] NULL_BYTE_CODE = { 0x60, (byte) 0xe5, 0x6d, 0x08, (byte) 0xd3, 0x08, 0x53,
            (byte) 0xb7, (byte) 0x84, 0x07, 0x77 };

    /**
     * @param object {@link RevObject} to hash.
     * @return {@code this}
     */
    public HashObject setObject(RevObject object) {
        this.object = object;
        return this;
    }

    /**
     * Hashes a RevObject using a SHA1 hasher.
     * 
     * @return a new ObjectId created from the hash of the RevObject.
     */
    @Override
    public ObjectId call() {
        Preconditions.checkState(object != null, "Object has not been set.");

        Hasher hasher = Hashing.sha1().newHasher();

        try {
            hashObject(hasher);
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        final byte[] rawKey = hasher.hash().asBytes();
        final ObjectId id = new ObjectId(rawKey);

        return id;
    }

    private void hashObject(final Hasher hasher) throws IOException {
        hasher.putInt(object.getType().value());
        switch (object.getType()) {
        case COMMIT:
            hashCommit(hasher, (RevCommit) object);
            break;
        case TREE:
            hashTree(hasher, (RevTree) object);
            break;
        case FEATURE:
            hashFeature(hasher, (RevFeature) object);
            break;
        case TAG:
            hashTag(hasher, (RevTag) object);
            break;
        case FEATURETYPE:
            hashFeatureType(hasher, (RevFeatureType) object);
            break;
        }
    }

    private void hashCommit(final Hasher hasher, RevCommit commit) throws IOException {

        hashObjectId(hasher, commit.getTreeId());

        List<ObjectId> parentIds = commit.getParentIds();
        hasher.putInt(parentIds.size());
        for (ObjectId pId : parentIds) {
            hashObjectId(hasher, pId);
        }

        hashPerson(hasher, commit.getAuthor());
        hashPerson(hasher, commit.getCommitter());
        if (commit.getMessage() == null) {
            hasher.putBytes(NULL_BYTE_CODE);
        } else {
            hasher.putString(commit.getMessage());
        }
        long timestamp = commit.getTimestamp();
        if (timestamp <= 0) {
            timestamp = System.currentTimeMillis();
        }
        hasher.putLong(timestamp);
    }

    private void hashFeature(final Hasher hasher, RevFeature feature) throws IOException {

        hasher.putInt(feature.getValues().size());
        for (Optional<Object> value : feature.getValues()) {
            hashProperty(hasher, value.orNull());
        }
    }

    private void hashTree(final Hasher hasher, RevTree revTree) throws IOException {
        if (!revTree.isNormalized()) {
            revTree = revTree.mutable();
            ((MutableTree) revTree).normalize();
        }

        TreeVisitor v = new WritingTreeVisitor(hasher);
        revTree.accept(v);
        hasher.putInt(TreeNode.END.getValue());
    }

    private void hashTag(final Hasher hasher, RevTag feature) {
        throw new UnsupportedOperationException();
    }

    private void hashFeatureType(final Hasher hasher, RevFeatureType featureType) {
        ImmutableSortedSet<PropertyDescriptor> featureTypeProperties = command(
                DescribeFeatureType.class).setFeatureType(featureType).call();

        hasher.putString(featureType.getName().getNamespaceURI() == null ? "" : featureType
                .getName().getNamespaceURI());
        hasher.putString(featureType.getName().getLocalPart());

        for (PropertyDescriptor descriptor : featureTypeProperties) {
            hashDescriptor(hasher, descriptor);
        }

    }

    private void hashObjectId(final Hasher hasher, ObjectId id) throws IOException {
        Preconditions.checkNotNull(id);
        if (id.isNull()) {
            hasher.putBytes(NULL_BYTE_CODE);
        } else {
            hasher.putBytes(id.getRawValue());
        }
    }

    private void hashPerson(final Hasher hasher, RevPerson person) throws IOException {
        if (person != null) {
            if (person.getName() != null) {
                hasher.putString(person.getName());
            } else {
                hasher.putBytes(NULL_BYTE_CODE);
            }
            if (person.getEmail() != null) {
                hasher.putString(person.getEmail());
            } else {
                hasher.putBytes(NULL_BYTE_CODE);
            }
        } else {
            hasher.putBytes(NULL_BYTE_CODE);
            hasher.putBytes(NULL_BYTE_CODE);
        }
    }

    private void hashProperty(final Hasher hasher, Object value) throws IOException {
        if (value == null) {
            hasher.putBytes(NULL_BYTE_CODE);
        } else if (value instanceof String) {
            hasher.putString((String) value);
        } else if (value instanceof Boolean) {
            hasher.putBoolean(((Boolean) value).booleanValue());
        } else if (value instanceof Byte) {
            hasher.putByte(((Byte) value).byteValue());
        } else if (value instanceof Double) {
            hasher.putDouble(((Double) value).doubleValue());
        } else if (value instanceof BigDecimal) {
            String bdString = ((BigDecimal) value).toEngineeringString();
            hasher.putString(bdString);
        } else if (value instanceof Float) {
            hasher.putFloat(((Float) value).floatValue());
        } else if (value instanceof Integer) {
            hasher.putInt(((Integer) value).intValue());
        } else if (value instanceof BigInteger) {
            byte[] bigBytes = ((BigInteger) value).toByteArray();
            hasher.putBytes(bigBytes);
        } else if (value instanceof Long) {
            hasher.putLong(((Long) value).longValue());
        } else if (value instanceof boolean[]) {
            boolean[] bools = (boolean[]) value;
            hasher.putInt(bools.length);
            for (boolean bool : bools) {
                hasher.putBoolean(bool);
            }
        } else if (value instanceof byte[]) {
            hasher.putBytes((byte[]) value);
        } else if (value instanceof char[]) {
            String chars = new String((char[]) value);
            hasher.putString(chars);
        } else if (value instanceof double[]) {
            double[] doubles = (double[]) value;
            hasher.putInt(doubles.length);
            for (double d : doubles) {
                hasher.putDouble(d);
            }
        } else if (value instanceof float[]) {
            float[] floats = (float[]) value;
            hasher.putInt(floats.length);
            for (float f : floats) {
                hasher.putFloat(f);
            }
        } else if (value instanceof int[]) {
            int[] ints = (int[]) value;
            hasher.putInt(ints.length);
            for (int i : ints) {
                hasher.putInt(i);
            }
        } else if (value instanceof long[]) {
            long[] longs = (long[]) value;
            hasher.putInt(longs.length);
            for (long l : longs) {
                hasher.putLong(l);
            }
        } else if (value instanceof java.util.UUID) {
            UUID uuid = (UUID) value;
            long most = uuid.getMostSignificantBits();
            long least = uuid.getLeastSignificantBits();
            hasher.putLong(most);
            hasher.putLong(least);
        } else if (value instanceof Geometry) {
            Geometry geom = (Geometry) value;
            String srs;
            if (geom.getUserData() instanceof CoordinateReferenceSystem) {
                srs = CRS.toSRS((CoordinateReferenceSystem) geom.getUserData());
            } else {
                srs = "urn.ogc.def.crs.EPSG::4326";
            }
            hasher.putString(srs);
            /*
             * The output streaming cleverness is modelled on Gabriel Roldans approach of ensuring
             * that we are streaming the wkb definition of the geometry instead of producing a
             * complete byte[].
             * 
             * The bugs and typos are my own.
             */
            final double scale = 1E9D;
            CoordinateFilter filter = new CoordinateFilter() {

                @Override
                public void filter(Coordinate coord) {
                    double x = Math.round(coord.x * scale) / scale;
                    double y = Math.round(coord.y * scale) / scale;
                    hasher.putDouble(x);
                    hasher.putDouble(y);
                }
            };
            geom.apply(filter);

        } else if (value instanceof Serializable) {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutput objectOut = null;

            objectOut = new ObjectOutputStream(byteOutput);
            objectOut.writeObject(value);
            hasher.putBytes(byteOutput.toByteArray());

            objectOut.close();
            byteOutput.close();
        } else {
            hasher.putString(value.getClass().getName());
            hasher.putString(value.toString());
        }

    }

    private void hashDescriptor(final Hasher hasher, PropertyDescriptor descriptor) {
        PropertyType attrType = descriptor.getType();
        GtEntityType type = GtEntityType.fromBinding(attrType.getBinding());
        hasher.putInt(type.getValue());
        hasher.putBoolean(descriptor.isNillable());
        Name propertyName = descriptor.getName();
        hasher.putString(propertyName.getNamespaceURI() == null ? "" : propertyName
                .getNamespaceURI());
        hasher.putString(propertyName.getLocalPart());
        hasher.putInt(descriptor.getMaxOccurs());
        hasher.putInt(descriptor.getMinOccurs());
        Name typeName = attrType.getName();
        hasher.putString(typeName.getNamespaceURI() == null ? "" : typeName.getNamespaceURI());
        hasher.putString(typeName.getLocalPart());
        if (type.equals(GtEntityType.GEOMETRY) && attrType instanceof GeometryType) {
            GeometryType gt = (GeometryType) attrType;
            hasher.putString(gt.getBinding().getName());
            hasher.putString(gt.getBinding().toString());
            CoordinateReferenceSystem crs = gt.getCoordinateReferenceSystem();
            String srsName;
            if (crs == null) {
                srsName = "urn:ogc:def:crs:EPSG::0";
            } else {
                srsName = CRS.toSRS(crs);
            }
            if (srsName != null) {
                hasher.putBoolean(true);
                hasher.putString(srsName);
            } else {
                String wkt;
                if (crs instanceof Formattable) {
                    wkt = ((Formattable) crs).toWKT(Formattable.SINGLE_LINE);
                } else {
                    wkt = crs.toWKT();
                }
                hasher.putBoolean(false);
                hasher.putString(wkt);
            }
        }
    }

    private void hashNodeRef(final Hasher hasher, NodeRef ref) throws IOException {
        BoundingBox bounds = null;
        if (ref instanceof SpatialRef) {
            bounds = ((SpatialRef) ref).getBounds();
        }
        hasher.putInt(TreeNode.REF.getValue());
        hasher.putInt(ref.getType().value());
        hasher.putString(ref.getPath());
        hashObjectId(hasher, ref.getObjectId());
        hashObjectId(hasher, ref.getMetadataId());
        hashBBox(hasher, bounds);
    }

    private void hashBBox(final Hasher hasher, BoundingBox bbox) throws IOException {
        if (bbox == null) {
            hasher.putDouble(Double.NaN);
            return;
        }
        CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
        String epsgCode;
        if (crs == null) {
            epsgCode = "";
        } else {
            epsgCode = lookupIdentifier(crs);
        }

        hasher.putDouble(bbox.getMinX());
        hasher.putDouble(bbox.getMaxX());
        hasher.putDouble(bbox.getMinY());
        hasher.putDouble(bbox.getMaxY());

        hasher.putString(epsgCode);
    }

    private String lookupIdentifier(CoordinateReferenceSystem crs) {
        String epsgCode = crsIdCache.get(crs);
        if (epsgCode == null) {
            try {
                epsgCode = CRS.toSRS(crs);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            if (epsgCode == null) {
                throw new IllegalArgumentException("Can't find EPSG code for CRS " + crs.toWKT());
            }
            crsIdCache.put(crs, epsgCode);
        }
        return epsgCode;
    }

    private final class WritingTreeVisitor implements TreeVisitor {
        private Hasher hasher;

        public WritingTreeVisitor(Hasher hasher) {
            this.hasher = hasher;
        }

        @Override
        public boolean visitEntry(NodeRef ref) {
            try {
                hashNodeRef(hasher, ref);
            } catch (IOException ex) {
                Throwables.propagate(ex);
            }
            return true;
        }

        @Override
        public boolean visitSubTree(int bucket, ObjectId treeId) {
            try {
                hasher.putInt(TreeNode.TREE.getValue());
                hasher.putInt(bucket);
                hashObjectId(hasher, treeId);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }
}
