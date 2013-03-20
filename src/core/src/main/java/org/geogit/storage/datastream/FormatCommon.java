/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.datastream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.geogit.api.Bucket;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTag;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeImpl;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.BasicFeatureTypes;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

public class FormatCommon {
    public final static byte NUL = 0x00;

    public final static String readToMarker(DataInput in, byte marker) throws IOException {
        StringBuilder buff = new StringBuilder();
        byte b = in.readByte();
        while (b != marker) {
            buff.append((char) b);
            b = in.readByte();
        }
        return buff.toString();
    }

    public final static void requireHeader(DataInput in, String header) throws IOException {
        String s = readToMarker(in, NUL);
        if (!header.equals(s))
            throw new IllegalArgumentException("Expected header " + header + ", but actually got "
                    + s);
    }

    public final static ObjectId readObjectId(DataInput in) throws IOException {
        byte[] bytes = new byte[20];
        in.readFully(bytes);
        return new ObjectId(bytes);
    }

    public static final byte COMMIT_TREE_REF = 0x01;

    public static final byte COMMIT_PARENT_REF = 0x02;

    public static final byte COMMIT_AUTHOR_PREFIX = 0x03;

    public static final byte COMMIT_COMMITTER_PREFIX = 0x04;

    /**
     * Constant for reading TREE objects. Indicates that the end of the tree object has been
     * reached.
     */
    public static final byte NO_MORE_NODES = 0x00;

    /**
     * Constant for reading TREE objects. Indicates that the next entry is a subtree node or a
     * features node.
     */
    public static final byte NODE = 0x01;

    /**
     * Constant for reading TREE objects. Indicates that the next entry is a bucket.
     */
    public static final byte BUCKET = 0x02;

    /**
     * The featuretype factory to use when calling code does not provide one.
     */
    private static final FeatureTypeFactory DEFAULT_FEATURETYPE_FACTORY = new SimpleFeatureTypeBuilder()
            .getFeatureTypeFactory();

    public static enum FieldType {
        NULL(0x00, Void.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return null;
            }

            @Override
            public void write(Object obj, DataOutput out) {
                // NO-OP: There is no body for a NULL field
            }
        },
        BOOLEAN(0x01, Boolean.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readBoolean();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeBoolean((Boolean) field);
            }
        },
        BYTE(0x02, Byte.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readByte();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeByte((Byte) field);
            }
        },
        SHORT(0x03, Short.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readShort();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeShort((Short) field);
            }
        },
        INTEGER(0x04, Integer.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readInt();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt((Integer) field);
            }
        },
        LONG(0x05, Long.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readLong();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeLong((Long) field);
            }
        },
        FLOAT(0x06, Float.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readFloat();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeFloat((Float) field);
            }
        },
        DOUBLE(0x07, Double.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readDouble();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeDouble((Double) field);
            }
        },
        STRING(0x08, String.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return in.readUTF();
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeUTF((String) field);
            }
        },
        BOOLEAN_ARRAY(0x09, boolean[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                final int len = in.readInt();
                byte[] packed = new byte[(len + 7) / 8]; // we want to round up as long as i % 8 !=
                                                         // 0
                boolean[] bits = new boolean[len];

                int offset = 0;
                int remainingBits = len;
                while (remainingBits > 8) {
                    byte chunk = packed[offset / 8];
                    for (int i = 0; i < 8; i++) {
                        bits[offset + i] = (chunk & (128 >> i)) != 0;
                    }
                    offset += 8;
                    remainingBits -= 8;
                }
                if (remainingBits > 0) {
                    byte chunk = packed[packed.length - 1];
                    int bitN = 0;
                    while (remainingBits > 0) {
                        bits[offset + bitN] = (chunk & (128 >> bitN)) != 0;
                        remainingBits -= 1;
                        bitN += 1;
                    }
                }
                return bits;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                boolean[] bools = (boolean[]) field;
                byte[] bytes = new byte[(bools.length + 7) / 8];

                int index = 0;
                while (index < bytes.length) {
                    int bIndex = index * 8;
                    int chunk = 0;
                    int bitsInChunk = Math.min(bools.length - bIndex, 8);
                    for (int i = 0; i < bitsInChunk; i++) {
                        chunk |= (bools[bIndex + i] ? 0 : 1) << (7 - i);
                    }
                    bytes[index] = (byte) chunk;
                }

                data.writeInt(bools.length);
                data.write(bytes);
            }
        },
        BYTE_ARRAY(0x0A, byte[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                byte[] bytes = new byte[len];
                in.readFully(bytes);
                return bytes;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt(((byte[]) field).length);
                data.write((byte[]) field);
            }
        },
        SHORT_ARRAY(0x0B, short[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                short[] shorts = new short[len];
                for (int i = 0; i < len; i++) {
                    shorts[i] = in.readShort();
                }
                return shorts;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt(((short[]) field).length);
                for (short s : (short[]) field)
                    data.writeShort(s);
            }
        },
        INTEGER_ARRAY(0x0C, int[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                int[] ints = new int[len];
                for (int i = 0; i < len; i++) {
                    ints[i] = in.readInt();
                }
                return ints;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt(((int[]) field).length);
                for (int i : (int[]) field)
                    data.writeInt(i);
            }
        },
        LONG_ARRAY(0x0D, long[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                long[] longs = new long[len];
                for (int i = 0; i < len; i++) {
                    longs[i] = in.readLong();
                }
                return longs;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt(((long[]) field).length);
                for (long l : (long[]) field)
                    data.writeLong(l);
            }
        },
        FLOAT_ARRAY(0x0E, float[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                float[] floats = new float[len];
                for (int i = 0; i < len; i++) {
                    floats[i] = in.readFloat();
                }
                return floats;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt(((float[]) field).length);
                for (float f : (float[]) field)
                    data.writeFloat(f);
            }
        },
        DOUBLE_ARRAY(0x0F, double[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                double[] doubles = new double[len];
                for (int i = 0; i < len; i++) {
                    doubles[i] = in.readDouble();
                }
                return doubles;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt(((double[]) field).length);
                for (double d : (double[]) field)
                    data.writeDouble(d);
            }
        },
        STRING_ARRAY(0x10, String[].class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                String[] strings = new String[len];
                for (int i = 0; i < len; i++) {
                    strings[i] = in.readUTF();
                }
                return strings;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeInt(((String[]) field).length);
                for (String s : (String[]) field)
                    data.writeUTF(s);
            }
        },
        POINT(0x11, Point.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return (Point) GEOMETRY.read(in);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                GEOMETRY.write(field, data);
            }
        },
        LINESTRING(0x12, LineString.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return (LineString) GEOMETRY.read(in);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                GEOMETRY.write(field, data);
            }
        },
        POLYGON(0x13, Polygon.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return (Polygon) GEOMETRY.read(in);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                GEOMETRY.write(field, data);
            }
        },
        MULTIPOINT(0x14, MultiPoint.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return (MultiPoint) GEOMETRY.read(in);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                GEOMETRY.write(field, data);
            }
        },
        MULTILINESTRING(0x15, MultiLineString.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return (MultiLineString) GEOMETRY.read(in);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                GEOMETRY.write(field, data);
            }
        },
        MULTIPOLYGON(0x16, MultiPolygon.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return (MultiPolygon) GEOMETRY.read(in);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                GEOMETRY.write(field, data);
            }
        },
        GEOMETRYCOLLECTION(0x17, GeometryCollection.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                return (GeometryCollection) GEOMETRY.read(in);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                GEOMETRY.write(field, data);
            }
        },
        GEOMETRY(0x18, Geometry.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                byte[] bytes = new byte[len]; // TODO: We should bound this to limit memory usage.
                in.readFully(bytes);
                WKBReader wkbReader = new WKBReader();
                try {
                    return wkbReader.read(bytes);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                WKBWriter wkbWriter = new WKBWriter();
                byte[] bytes = wkbWriter.write((Geometry) field);
                BYTE_ARRAY.write(bytes, data);
            }
        },
        UUID(0x19, java.util.UUID.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                long upper = in.readLong();
                long lower = in.readLong();
                return new java.util.UUID(upper, lower);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                data.writeLong(((java.util.UUID) field).getMostSignificantBits());
                data.writeLong(((java.util.UUID) field).getLeastSignificantBits());
            }
        },
        BIG_INTEGER(0x1A, BigInteger.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int len = in.readInt();
                byte[] bytes = new byte[len];
                in.readFully(bytes);
                return new BigInteger(bytes);
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                byte[] bytes = ((BigInteger) field).toByteArray();
                BYTE_ARRAY.write(bytes, data);
            }
        },
        BIG_DECIMAL(0x1B, BigDecimal.class) {
            @Override
            public Object read(DataInput in) throws IOException {
                int scale = in.readInt();
                int len = in.readInt();
                byte[] bytes = new byte[len];
                in.readFully(bytes);
                BigInteger intValue = new BigInteger(bytes);
                BigDecimal decValue = new BigDecimal(intValue, scale);
                return decValue;
            }

            @Override
            public void write(Object field, DataOutput data) throws IOException {
                BigDecimal d = (BigDecimal) field;
                int scale = d.scale();
                BigInteger i = d.unscaledValue();
                data.writeInt(scale);
                BIG_INTEGER.write(i, data);
            }
        };

        private final byte tagValue;

        private final Class<?> binding;

        FieldType(int tagValue, Class<?> binding) {
            this.tagValue = (byte) tagValue;
            this.binding = binding;
        }

        public Class<?> getBinding() {
            return binding;
        }

        public byte getTag() {
            return tagValue;
        }

        public static FieldType valueOf(int i) {
            return values()[i];
        }

        public abstract Object read(DataInput in) throws IOException;

        public static FieldType forValue(Optional<Object> field) {
            if (field.isPresent()) {
                Object realField = field.get();
                for (FieldType t : values()) {
                    if (t.getBinding().isInstance(realField))
                        return t;
                }
                throw new IllegalArgumentException("Attempted to write unsupported field type "
                        + realField.getClass());
            } else {
                return NULL;
            }
        }

        public static FieldType forBinding(Class<?> binding) {
            for (FieldType t : values()) {
                if (t.getBinding().isAssignableFrom(binding)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Attempted to write unsupported field type "
                    + binding);
        }

        public abstract void write(Object field, DataOutput data) throws IOException;
    }

    public static RevTag readTag(ObjectId id, DataInput in) throws IOException {
        final ObjectId commitId = readObjectId(in);
        final String name = in.readUTF();
        final String message = in.readUTF();
        final RevPerson tagger = readRevPerson(in);

        return new RevTag(id, name, commitId, message, tagger);
    }

    public static void writeTag(RevTag tag, DataOutput out) throws IOException {
        out.write(tag.getCommitId().getRawValue());
        out.writeUTF(tag.getName());
        out.writeUTF(tag.getMessage());
        writePerson(tag.getTagger(), out);
    }

    public static RevCommit readCommit(ObjectId id, DataInput in) throws IOException {
        byte tag = in.readByte();
        if (tag != COMMIT_TREE_REF) {
            throw new IllegalArgumentException("Commit should include a tree ref");
        }

        final byte[] treeIdBytes = new byte[20];
        in.readFully(treeIdBytes);
        final ObjectId treeId = new ObjectId(treeIdBytes);
        final Builder<ObjectId> parentListBuilder = ImmutableList.builder();

        while (true) {
            tag = in.readByte();
            if (tag != COMMIT_PARENT_REF) {
                break;
            } else {
                final byte[] parentIdBytes = new byte[20];
                in.readFully(parentIdBytes);
                parentListBuilder.add(new ObjectId(parentIdBytes));
            }
        }

        if (tag != COMMIT_AUTHOR_PREFIX) {
            throw new IllegalArgumentException(
                    "Expected AUTHOR element following parent ids in commit");
        }

        final RevPerson author = readRevPerson(in);

        tag = in.readByte();
        if (tag != COMMIT_COMMITTER_PREFIX) {
            throw new IllegalArgumentException(
                    "Expected COMMITTER element following author in commit");
        }

        final RevPerson committer = readRevPerson(in);

        final String message = in.readUTF();

        return new RevCommit(id, treeId, parentListBuilder.build(), author, committer, message);
    }

    public static final RevPerson readRevPerson(DataInput in) throws IOException {
        final String name = in.readUTF();
        final String email = in.readUTF();
        final long timestamp = in.readLong();
        final int tzOffset = in.readInt();
        return new RevPerson(name.length() == 0 ? null : name, email.length() == 0 ? null : email,
                timestamp, tzOffset);
    }

    public static final void writePerson(RevPerson person, DataOutput data) throws IOException {
        data.writeUTF(person.getName().or(""));
        data.writeUTF(person.getEmail().or(""));
        data.writeLong(person.getTimestamp());
        data.writeInt(person.getTimeZoneOffset());
    }

    public static RevTree readTree(ObjectId id, DataInput in) throws IOException {
        final long size = in.readLong();
        final int treeCount = in.readInt();
        final List<Node> features = new ArrayList<Node>();
        final List<Node> trees = new ArrayList<Node>();
        final SortedMap<Integer, Bucket> buckets = new TreeMap<Integer, Bucket>();

        final int nFeatures = in.readInt();
        for (int i = 0; i < nFeatures; i++) {
            Node n = readNode(in);
            if (n.getType() != RevObject.TYPE.FEATURE) {
                throw new IllegalStateException("Non-feature node in tree's feature list.");
            }
            features.add(n);
        }

        final int nTrees = in.readInt();
        for (int i = 0; i < nTrees; i++) {
            Node n = readNode(in);
            if (n.getType() != RevObject.TYPE.TREE) {
                throw new IllegalStateException("Non-tree node in tree's subtree list.");
            }
            trees.add(n);
        }

        final int nBuckets = in.readInt();
        for (int i = 0; i < nBuckets; i++) {
            int key = in.readInt();
            Bucket bucket = readBucket(in);
            buckets.put(key, bucket);
        }

        if (trees.isEmpty() && features.isEmpty()) {
            return RevTreeImpl.createNodeTree(id, size, treeCount, buckets);
        } else if (buckets.isEmpty()) {
            return RevTreeImpl.createLeafTree(id, size, features, trees);
        } else {
            throw new IllegalArgumentException(
                    "Tree has mixed buckets and nodes; this is not supported.");
        }
    }

    public static Node readNode(DataInput in) throws IOException {
        final String name = in.readUTF();
        final byte[] objectId = new byte[20];
        in.readFully(objectId);
        final byte[] metadataId = new byte[20];
        in.readFully(metadataId);
        final RevObject.TYPE contentType = RevObject.TYPE.valueOf(in.readByte());
        final Envelope bbox = readBBox(in);
        final Node node;
        if (!bbox.isNull()) {
            node = Node.create(name, new ObjectId(objectId), new ObjectId(metadataId), contentType,
                    bbox);
        } else {
            node = Node.create(name, new ObjectId(objectId), new ObjectId(metadataId), contentType);
        }
        return node;
    }

    public static final Bucket readBucket(DataInput in) throws IOException {
        final byte[] hash = new byte[20];
        in.readFully(hash);
        ObjectId objectId = new ObjectId(hash);
        Envelope bounds = readBBox(in);
        return Bucket.create(objectId, bounds);
    }

    private static Envelope readBBox(DataInput in) throws IOException {
        final double minx = in.readDouble();
        final double maxx = in.readDouble();
        final double miny = in.readDouble();
        final double maxy = in.readDouble();
        return new Envelope(minx, maxx, miny, maxy);
    }

    public static RevFeature readFeature(ObjectId id, DataInput in) throws IOException {
        final int count = in.readInt();
        final ImmutableList.Builder<Optional<Object>> builder = ImmutableList.builder();

        for (int i = 0; i < count; i++) {
            final byte fieldTag = in.readByte();
            final FieldType fieldType = FieldType.valueOf(fieldTag);
            builder.add(Optional.fromNullable(fieldType.read(in)));
        }

        return new RevFeature(id, builder.build());
    }

    public static RevFeatureType readFeatureType(ObjectId id, DataInput in) throws IOException {
        return readFeatureType(id, in, DEFAULT_FEATURETYPE_FACTORY);
    }

    public static RevFeatureType readFeatureType(ObjectId id, DataInput in,
            FeatureTypeFactory typeFactory) throws IOException {
        Name name = readName(in);
        int propertyCount = in.readInt();
        List<AttributeDescriptor> attributes = new ArrayList<AttributeDescriptor>();
        for (int i = 0; i < propertyCount; i++) {
            attributes.add(readAttributeDescriptor(in, typeFactory));
        }
        SimpleFeatureType ftype = typeFactory.createSimpleFeatureType(name, attributes, null,
                false, Collections.<Filter> emptyList(), BasicFeatureTypes.FEATURE, null);
        return new RevFeatureType(id, ftype);
    }

    private static Name readName(DataInput in) throws IOException {
        String namespace = in.readUTF();
        String localPart = in.readUTF();
        return new NameImpl(namespace.length() == 0 ? null : namespace,
                localPart.length() == 0 ? null : localPart);
    }

    private static AttributeType readAttributeType(DataInput in, FeatureTypeFactory typeFactory)
            throws IOException {
        final Name name = readName(in);
        final byte typeTag = in.readByte();
        final FieldType type = FieldType.valueOf(typeTag);
        if (Geometry.class.isAssignableFrom(type.getBinding())) {
            final boolean isCRSCode = in.readBoolean(); // as opposed to a raw WKT string
            final String crsText = in.readUTF();
            final CoordinateReferenceSystem crs;
            try {
                if (isCRSCode) {
                    if ("urn:ogc:def:crs:EPSG::0".equals(crsText)) {
                        crs = null;
                    } else {
                        boolean forceLongitudeFirst = crsText.startsWith("EPSG:");
                        crs = CRS.decode(crsText, forceLongitudeFirst);
                    }
                } else {
                    crs = CRS.parseWKT(crsText);
                }
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            }
            return typeFactory.createGeometryType(name, type.getBinding(), crs, false, false,
                    Collections.<Filter> emptyList(), null, null);
        } else {
            return typeFactory.createAttributeType(name, type.getBinding(), false, false,
                    Collections.<Filter> emptyList(), null, null);
        }
    }

    private static AttributeDescriptor readAttributeDescriptor(DataInput in,
            FeatureTypeFactory typeFactory) throws IOException {
        final Name name = readName(in);
        final boolean nillable = in.readBoolean();
        final int minOccurs = in.readInt();
        final int maxOccurs = in.readInt();
        final AttributeType type = readAttributeType(in, typeFactory);
        if (type instanceof GeometryType)
            return typeFactory.createGeometryDescriptor((GeometryType) type, name, minOccurs,
                    maxOccurs, nillable, null);
        else
            return typeFactory.createAttributeDescriptor(type, name, minOccurs, maxOccurs,
                    nillable, null);
    }

    public static void writeHeader(DataOutput data, String header) throws IOException {
        byte[] bytes = header.getBytes(Charset.forName("US-ASCII"));
        data.write(bytes);
        data.writeByte(NUL);
    }

    public static void writeBoundingBox(Envelope bbox, DataOutput data) throws IOException {
        data.writeDouble(bbox.getMinX());
        data.writeDouble(bbox.getMaxX());
        data.writeDouble(bbox.getMinY());
        data.writeDouble(bbox.getMaxY());
    }

    public static void writeBucket(int index, Bucket bucket, DataOutput data) throws IOException {
        data.writeInt(index);
        data.write(bucket.id().getRawValue());
        Envelope e = new Envelope();
        bucket.expand(e);
        writeBoundingBox(e, data);
    }

    public static void writeNode(Node node, DataOutput data) throws IOException {
        data.writeUTF(node.getName());
        data.write(node.getObjectId().getRawValue());
        data.write(node.getMetadataId().or(ObjectId.NULL).getRawValue());
        int typeN = node.getType().value();
        data.writeByte(typeN);
        Envelope envelope = new Envelope();
        node.expand(envelope);
        writeBoundingBox(envelope, data);
    }
}
