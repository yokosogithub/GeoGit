/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.collections.map.LRUMap;
import org.geogit.api.CommitBuilder;
import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeImpl;
import org.geogit.api.SpatialNode;
import org.geogit.storage.GtEntityType;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.ObjectWriter;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.wkt.Formattable;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;

/**
 * An {@link ObjectSerialisingFactory} for the {@link RevObject}s text format.
 * <p>
 * The following formats are used to interchange {@link RevObject} instances in a text format:
 * <H3>Commit:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * {@code "tree" + "\t" +  <tree id> + "\n"}
 * {@code "parents" + "\t" +  <parent id> [+ " " + <parent id>...]  + "\n"} 
 * {@code "author" + "\t" +  <<author name> | ""> + " " + "<" + <<author email> | ""> + ">\n"}
 * {@code "committer" + "\t" +  <<committer name> | ""> + " " + "<" + <<committer email> | ""> + ">\n"}
 * {@code "timestamp" + "\t" +  <timestamp> + "\n"}
 * {@code "message" + "\t" +  <message> + "\n"}
 * </pre>
 * 
 * <H3>Tree:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 * 
 * <H3>Feature:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 * 
 * <H3>FeatureType:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 * 
 * <H3>Tag:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 */
public class TextSerializationFactory implements ObjectSerialisingFactory {

    protected static final String NULL = "null";

    @Override
    public ObjectReader<RevCommit> createCommitReader() {
        return COMMIT_READER;
    }

    @Override
    public ObjectReader<RevTree> createRevTreeReader() {
        return TREE_READER;
    }

    @Override
    public ObjectReader<RevFeature> createFeatureReader() {
        return FEATURE_READER;
    }

    @Override
    public ObjectReader<RevFeature> createFeatureReader(Map<String, Serializable> hints) {
        // TODO
        return FEATURE_READER;
    }

    @Override
    public ObjectReader<RevFeatureType> createFeatureTypeReader() {
        return FEATURETYPE_READER;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RevObject> ObjectWriter<T> createObjectWriter(TYPE type) {
        switch (type) {
        case COMMIT:
            return (ObjectWriter<T>) COMMIT_WRITER;
        case FEATURE:
            return (ObjectWriter<T>) FEATURE_WRITER;
        case FEATURETYPE:
            return (ObjectWriter<T>) FEATURETYPE_WRITER;
        case TREE:
            return (ObjectWriter<T>) TREE_WRITER;
        default:
            throw new IllegalArgumentException("Unknown or unsupported object type: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ObjectReader<T> createObjectReader(TYPE type) {
        switch (type) {
        case COMMIT:
            return (ObjectReader<T>) COMMIT_READER;
        case FEATURE:
            return (ObjectReader<T>) FEATURE_READER;
        case FEATURETYPE:
            return (ObjectReader<T>) FEATURETYPE_READER;
        case TREE:
            return (ObjectReader<T>) TREE_READER;
        default:
            throw new IllegalArgumentException("Unknown or unsupported object type: " + type);
        }
    }

    @Override
    public ObjectReader<RevObject> createObjectReader() {
        return OBJECT_READER;
    }

    /**
     * Abstract text writer that provides print methods on a {@link Writer} to consistently write
     * newlines as {@code \n} instead of using the platform's line separator as in
     * {@link PrintWriter}. It also provides some common method used by different writers.
     */
    private static abstract class TextWriter<T extends RevObject> implements ObjectWriter<T> {

        public static final String NULL_BOUNDING_BOX = "null";

        @SuppressWarnings("unchecked")
        private static Map<CoordinateReferenceSystem, String> crsIdCache = Collections
                .synchronizedMap(new LRUMap(3));

        /**
         * Different types of tree nodes.
         */
        public enum TreeNode {
            REF, BUCKET;
        }

        @Override
        public void write(T object, OutputStream out) throws IOException {
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            println(writer, object.getType().name());
            print(object, writer);
            writer.flush();
        }

        protected abstract void print(T object, Writer w) throws IOException;

        protected void print(Writer w, CharSequence... s) throws IOException {
            if (s == null) {
                return;
            }
            for (CharSequence c : s) {
                w.write(String.valueOf(c));
            }
        }

        protected void println(Writer w, CharSequence... s) throws IOException {
            print(w, s);
            w.write('\n');
        }

        protected void writeNode(Writer w, Node node) throws IOException {
            BoundingBox bounds = null;
            if (node instanceof SpatialNode) {
                bounds = ((SpatialNode) node).getBounds();
            }
            print(w, TreeNode.REF.name());
            print(w, "\t");
            print(w, node.getType().name());
            print(w, "\t");
            print(w, node.getName());
            print(w, "\t");
            print(w, node.getObjectId().toString());
            print(w, "\t");
            print(w, node.getMetadataId().or(ObjectId.NULL).toString());
            print(w, "\t");
            writeBBox(w, bounds);
            println(w, "");
        }

        private void writeBBox(Writer w, BoundingBox bbox) throws IOException {
            if (bbox == null) {
                print(w, "null");
                return;
            }
            CoordinateReferenceSystem crs = bbox.getCoordinateReferenceSystem();
            String epsgCode;
            if (crs == null) {
                epsgCode = "";
            } else {
                epsgCode = lookupIdentifier(crs);
            }

            print(w, Double.toString(bbox.getMinX()));
            print(w, ";");
            print(w, Double.toString(bbox.getMaxX()));
            print(w, ";");
            print(w, Double.toString(bbox.getMinY()));
            print(w, ";");
            print(w, Double.toString(bbox.getMaxY()));
            print(w, ";");
            print(w, epsgCode);

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
                    throw new IllegalArgumentException("Can't find EPSG code for CRS "
                            + crs.toWKT());
                }
                crsIdCache.put(crs, epsgCode);
            }
            return epsgCode;
        }
    }

    /**
     * Commit writer.
     * <p>
     * Output format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "tree" + "\t" +  <tree id> + "\n"}
     * {@code "parents" + "\t" +  <parent id> [+ " " + <parent id>...]  + "\n"}
     * {@code "author" + "\t" +  <<author name> | ""> + " " + "<" + <<author email> | ""> + ">\n"}
     * {@code "committer" + "\t" +  <<committer name> | ""> + " " + "<" + <<committer email> | ""> + ">\n"}
     * {@code "timestamp" + "\t" +  <timestamp> + "\n"}
     * {@code "message" + "\t" +  <message> + "\n"}
     * </pre>
     * 
     */
    private static final TextWriter<RevCommit> COMMIT_WRITER = new TextWriter<RevCommit>() {

        @Override
        protected void print(RevCommit commit, Writer w) throws IOException {
            println(w, "id\t", commit.getId().toString());
            println(w, "tree\t", commit.getTreeId().toString());
            print(w, "parents\t");
            for (Iterator<ObjectId> it = commit.getParentIds().iterator(); it.hasNext();) {
                print(w, it.next().toString());
                if (it.hasNext()) {
                    print(w, " ");
                }
            }
            println(w);
            printPerson(w, "author", commit.getAuthor());
            printPerson(w, "committer", commit.getCommitter());
            println(w, "timestamp\t", String.valueOf(commit.getTimestamp()));
            println(w, "message\t", Optional.fromNullable(commit.getMessage()).or(""));
            w.flush();
        }

        private void printPerson(Writer w, String name, RevPerson person) throws IOException {
            print(w, name);
            print(w, "\t");
            print(w, Optional.fromNullable(person.getName()).or(""));
            print(w, " <");
            print(w, Optional.fromNullable(person.getEmail()).or(""));
            print(w, ">");
            println(w);
        }
    };

    /**
     * Feature writer.
     * <p>
     * Output format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "<attribute_class_1>" + "\t" +  <attribute_value_1> + "\n"}
     * .
     * .
     * .     
     * {@code "<attribute_class_n>" + "\t" +  <attribute_value_n> + "\n"}
     * For array types, values are written as a space-separated list of single values, enclosed between square brackets
     * </pre>
     * 
     */
    private static final TextWriter<RevFeature> FEATURE_WRITER = new TextWriter<RevFeature>() {

        @Override
        protected void print(RevFeature feature, Writer w) throws IOException {
            println(w, "id\t", feature.getId().toString());
            ImmutableList<Optional<Object>> values = feature.getValues();
            for (Optional<Object> opt : values) {
                if (opt.isPresent()) {
                    Object value = opt.get();
                    println(w, value.getClass().getName() + "\t",
                            AttributeValueSerializer.asText(value));
                } else {
                    println(w, NULL);
                }
            }
            w.flush();
        }

    };

    /**
     * Feature type writer.
     * <p>
     * Output format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "name" + "\t" +  <name> + "\n"}
     * {@code "<attribute_name>" + "\t" +  <attribute_type> + "\t" + <min_occur> + "\t" + <max_occur> +  "\t" + <nillable <True|False>> + "\n"}
     * {@code "<attribute_name>" + "\t" +  <attribute_type> + "\t" + <min_occur> + "\t" + <max_occur> +  "\t" + <nillable <True|False>> + "\n"}
     * .
     * .
     * .
     * 
     * </pre>
     * 
     * Geometry attributes have an extra token per line representing the crs
     * 
     */
    private static final TextWriter<RevFeatureType> FEATURETYPE_WRITER = new TextWriter<RevFeatureType>() {

        @Override
        protected void print(RevFeatureType featureType, Writer w) throws IOException {
            println(w, "id\t", featureType.getId().toString());
            println(w, "name\t", featureType.getName().toString());
            Collection<PropertyDescriptor> attribs = featureType.type().getDescriptors();
            for (PropertyDescriptor attrib : attribs) {
                printAttributeDescriptor(w, attrib);
            }
            w.flush();
        }

        private void printAttributeDescriptor(Writer w, PropertyDescriptor attrib)
                throws IOException {
            print(w, attrib.getName().toString());
            print(w, "\t");
            print(w, attrib.getType().getBinding().getName());
            print(w, "\t");
            print(w, Integer.toString(attrib.getMinOccurs()));
            print(w, "\t");
            print(w, Integer.toString(attrib.getMaxOccurs()));
            print(w, "\t");
            print(w, Boolean.toString(attrib.isNillable()));
            PropertyType attrType = attrib.getType();
            GtEntityType type = GtEntityType.fromBinding(attrType.getBinding());
            if (type.isGeometry() && attrType instanceof GeometryType) {
                GeometryType gt = (GeometryType) attrType;
                CoordinateReferenceSystem crs = gt.getCoordinateReferenceSystem();
                String srsName;
                if (crs == null) {
                    srsName = "urn:ogc:def:crs:EPSG::0";
                } else {
                    // use a flag to control whether the code is returned in EPSG: form instead of
                    // urn:ogc:.. form irrespective of the org.geotools.referencing.forceXY System
                    // property.
                    final boolean longitudeFisrt = CRS.getAxisOrder(crs, false) == AxisOrder.EAST_NORTH;
                    boolean codeOnly = true;
                    String crsCode = CRS.toSRS(crs, codeOnly);
                    if (crsCode != null) {
                        srsName = (longitudeFisrt ? "EPSG:" : "urn:ogc:def:crs:EPSG::") + crsCode;
                    } else {
                        srsName = null;
                    }
                }
                print(w, "\t");
                if (srsName != null) {
                    print(w, srsName, "\n");
                } else {
                    String wkt;
                    if (crs instanceof Formattable) {
                        wkt = ((Formattable) crs).toWKT(Formattable.SINGLE_LINE);
                    } else {
                        wkt = crs.toWKT();
                    }
                    println(w, wkt);
                }
            } else {
                println(w, "");
            }

        }

    };

    /**
     * Tree writer.
     * <p>
     * Output format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * 
     * {@code "BUCKET" + "\t" +  <bucket_idx> + "\t" + <ObjectId> + "\n"}
     * or 
     * {@code "REF" + "\t" +  <ref_type> + "\t" + <ref_name> + "\t" + <ObjectId> + "\t" + <MetadataId> + "\t" + <bounds>"\n"}
     * .
     * .
     * .
     * </pre>
     */
    private static final TextWriter<RevTree> TREE_WRITER = new TextWriter<RevTree>() {

        @Override
        protected void print(RevTree revTree, Writer w) throws IOException {
            println(w, "id\t", revTree.getId().toString());
            println(w, "size\t", Long.toString(revTree.size()));
            if (revTree.trees().isPresent()) {
                writeChildren(w, revTree.trees().get());
            }
            if (revTree.features().isPresent()) {
                writeChildren(w, revTree.features().get());
            } else if (revTree.buckets().isPresent()) {
                writeBuckets(w, revTree.buckets().get());
            }

        }

        private void writeChildren(Writer w, ImmutableCollection<Node> children) throws IOException {
            for (Node ref : children) {
                writeNode(w, ref);
            }
        }

        private void writeBuckets(Writer w, ImmutableSortedMap<Integer, ObjectId> buckets)
                throws IOException {

            for (Entry<Integer, ObjectId> entry : buckets.entrySet()) {
                print(w, TreeNode.BUCKET.name());
                print(w, "\t");
                print(w, Integer.toString(entry.getKey().intValue()));
                print(w, "\t");
                println(w, entry.getValue().toString());
            }
        }

    };

    private abstract static class TextReader<T extends RevObject> implements ObjectReader<T> {

        @SuppressWarnings("unchecked")
        private static Map<String, CoordinateReferenceSystem> crsCache = Collections
                .synchronizedMap(new LRUMap(3));

        @Override
        public T read(ObjectId id, InputStream rawData) throws IllegalArgumentException {
            try {
                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(rawData, "UTF-8"));
                TYPE type = RevObject.TYPE.valueOf(reader.readLine().trim());
                T parsed = read(reader, type);
                Preconditions.checkState(parsed != null, "parsed to null");
                Preconditions.checkState(id.equals(parsed.getId()),
                        "Expected and parsed object ids don't match: %s %s", id, parsed.getId());
                return parsed;
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

        protected String parseLine(String line, String expectedHeader) throws IOException {
            List<String> fields = Lists.newArrayList(Splitter.on('\t').split(line));
            Preconditions.checkArgument(fields.size() == 2, "Expected %s\\t<...>, got '%s'",
                    expectedHeader, line);
            Preconditions.checkArgument(expectedHeader.equals(fields.get(0)),
                    "Expected field %s, got '%s'", expectedHeader, fields.get(0));
            String value = fields.get(1);
            return value;
        }

        protected abstract T read(BufferedReader reader, TYPE type) throws IOException;

        protected Node parseNodeLine(String line) {
            List<String> tokens = Lists.newArrayList(Splitter.on('\t').split(line));
            Preconditions.checkArgument(tokens.size() == 6, "Wrong tree element definition: %s",
                    line);
            TYPE type = TYPE.valueOf(tokens.get(1));
            String name = tokens.get(2);
            ObjectId id = ObjectId.valueOf(tokens.get(3));
            ObjectId metadataId = ObjectId.valueOf(tokens.get(4));
            BoundingBox bbox = parseBBox(tokens.get(5));

            org.geogit.api.Node ref;
            if (bbox == null) {
                ref = new org.geogit.api.Node(name, id, metadataId, type);
            } else {
                ref = new SpatialNode(name, id, metadataId, type, bbox);
            }

            return ref;

        }

        private BoundingBox parseBBox(String s) {
            if (s.equals(TextWriter.NULL_BOUNDING_BOX)) {
                return null;
            }
            List<String> tokens = Lists.newArrayList(Splitter.on(';').split(s));
            Preconditions.checkArgument(tokens.size() == 5, "Wrong bounding box definition: %s", s);

            double minx = Double.parseDouble(tokens.get(0));
            double maxx = Double.parseDouble(tokens.get(1));
            double miny = Double.parseDouble(tokens.get(2));
            double maxy = Double.parseDouble(tokens.get(3));

            String epsgCode = tokens.get(4);
            CoordinateReferenceSystem crs = null;
            if (epsgCode != null && epsgCode.length() > 0)
                crs = lookupCrs(epsgCode);

            BoundingBox bbox = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
            return bbox;
        }

        private static CoordinateReferenceSystem lookupCrs(final String epsgCode) {
            CoordinateReferenceSystem crs = crsCache.get(epsgCode);
            if (crs == null) {
                try {
                    crs = CRS.decode(epsgCode, false);
                    crsCache.put(epsgCode, crs);
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
            return crs;
        }
    }

    private static final TextReader<RevObject> OBJECT_READER = new TextReader<RevObject>() {

        @Override
        protected RevObject read(BufferedReader read, TYPE type) throws IOException {
            switch (type) {
            case COMMIT:
                return COMMIT_READER.read(read, type);
            case FEATURE:
                return FEATURE_READER.read(read, type);
            case TREE:
                return TREE_READER.read(read, type);
            case FEATURETYPE:
                return FEATURETYPE_READER.read(read, type);
            default:
                throw new IllegalArgumentException("Unknown object type " + type);
            }
        }

    };

    /**
     * Commit reader.
     * <p>
     * Parses a commit of the format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "tree" + "\t" +  <tree id> + "\n"}
     * {@code "parents" + "\t" +  <parent id> [+ " " + <parent id>...]  + "\n"}
     * {@code "author" + "\t" +  <<author name> | ""> + " " + "<" + <<author email> | ""> + ">\n"}
     * {@code "committer" + "\t" +  <<committer name> | ""> + " " + "<" + <<committer email> | ""> + ">\n"}
     * {@code "timestamp" + "\t" +  <timestamp> + "\n"}
     * {@code "message" + "\t" +  <message> + "\n"}
     * </pre>
     * 
     */
    private static final TextReader<RevCommit> COMMIT_READER = new TextReader<RevCommit>() {

        @Override
        protected RevCommit read(BufferedReader reader, TYPE type) throws IOException {
            Preconditions.checkArgument(TYPE.COMMIT.equals(type), "Wrong type: %s", type.name());
            String id = parseLine(reader.readLine(), "id");
            String tree = parseLine(reader.readLine(), "tree");
            List<String> parents = Lists.newArrayList(Splitter.on(' ').omitEmptyStrings()
                    .split(parseLine(reader.readLine(), "parents")));
            RevPerson author = parsePerson(reader.readLine(), "author");
            RevPerson committer = parsePerson(reader.readLine(), "committer");
            String timeStamp = parseLine(reader.readLine(), "timestamp");
            String message = parseMessage(reader);

            CommitBuilder builder = new CommitBuilder();
            builder.setAuthor(author.getName());
            builder.setAuthorEmail(author.getEmail());
            builder.setCommitter(committer.getName());
            builder.setCommitterEmail(committer.getEmail());
            builder.setMessage(message);
            List<ObjectId> parentIds = Lists.newArrayList(Iterators.transform(parents.iterator(),
                    new Function<String, ObjectId>() {

                        @Override
                        public ObjectId apply(String input) {
                            ObjectId objectId = ObjectId.valueOf(input);
                            return objectId;
                        }
                    }));
            builder.setParentIds(parentIds);
            builder.setTreeId(ObjectId.valueOf(tree));
            builder.setTimestamp(Long.parseLong(timeStamp));
            RevCommit commit = builder.build();
            ObjectId oid = ObjectId.valueOf(id);
            // Preconditions.checkArgument(oid.equals(commit.getId()));
            return commit;
        }

        private RevPerson parsePerson(String line, String expectedHeaderName) throws IOException {
            String value = parseLine(line, expectedHeaderName);
            int emailStart = value.indexOf('<');
            String name = value.substring(0, emailStart).trim();
            String email = value.substring(emailStart + 1, value.length() - 1).trim();
            if (name.isEmpty()) {
                name = null;
            }
            if (email.isEmpty()) {
                email = null;
            }
            return new RevPerson(name, email);
        }

        private String parseMessage(BufferedReader reader) throws IOException {
            StringBuilder msg = new StringBuilder(parseLine(reader.readLine(), "message"));
            String extraLine;
            while ((extraLine = reader.readLine()) != null) {
                msg.append('\n').append(extraLine);
            }
            return msg.toString();
        }

    };

    /**
     * Feature reader.
     * <p>
     * Parses a feature in the format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "<attribute class_1>" + "\t" +  <attribute_value_1> + "\n"}
     * .
     * .
     * .     
     * {@code "<attribute class_n>" + "\t" +  <attribute_value_n> + "\n"}
     * 
     * Array values are written as a space-separated list of single values, enclosed between square brackets
     * </pre>
     * 
     */
    private static final TextReader<RevFeature> FEATURE_READER = new TextReader<RevFeature>() {

        @Override
        protected RevFeature read(BufferedReader reader, TYPE type) throws IOException {
            Preconditions.checkArgument(TYPE.FEATURE.equals(type), "Wrong type: %s", type.name());
            String id = parseLine(reader.readLine(), "id");
            List<Object> values = Lists.newArrayList();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> tokens = Lists.newArrayList(Splitter.on('\t').split(line));
                if (tokens.size() == 2) {
                    values.add(AttributeValueSerializer.fromText(tokens.get(0), tokens.get(1)));
                } else if (tokens.size() == 1 && tokens.get(0).trim().equals(NULL)) {
                    values.add(null);
                } else {
                    throw new IllegalArgumentException("Wrong attribute definition: " + line);
                }
            }

            ImmutableList.Builder<Optional<Object>> valuesBuilder = new ImmutableList.Builder<Optional<Object>>();
            for (Object value : values) {
                valuesBuilder.add(Optional.fromNullable(value));
            }
            return new RevFeature(ObjectId.valueOf(id), valuesBuilder.build());
        }

    };

    /**
     * Feature type reader.
     * <p>
     * Parses a feature type in the format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "name" + "\t" +  <name> + "\n"}
     * {@code "<attribute_name1>" + "\t" +  <attribute_class1> + "\t" + <min_occur> + "\t" + <max_occur> +  "\n" + <nillable <True|False>>}
     * .
     * .
     * .
     * 
     * </pre>
     * 
     * Geometry attributes have an extra token per line representing the crs
     * 
     */
    private static final TextReader<RevFeatureType> FEATURETYPE_READER = new TextReader<RevFeatureType>() {

        private SimpleFeatureTypeBuilder builder;

        private FeatureTypeFactory typeFactory;

        @Override
        protected RevFeatureType read(BufferedReader reader, TYPE type) throws IOException {
            Preconditions.checkArgument(TYPE.FEATURETYPE.equals(type), "Wrong type: %s",
                    type.name());
            builder = new SimpleFeatureTypeBuilder();
            typeFactory = builder.getFeatureTypeFactory();
            String id = parseLine(reader.readLine(), "id");
            String name = parseLine(reader.readLine(), "name");
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName(new NameImpl(name));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.add(parseAttributeDescriptor(line));
            }
            SimpleFeatureType sft = builder.buildFeatureType();
            return new RevFeatureType(ObjectId.valueOf(id), sft);

        }

        private AttributeDescriptor parseAttributeDescriptor(String line) {
            ArrayList<String> tokens = Lists.newArrayList(Splitter.on('\t').split(line));
            Preconditions.checkArgument(tokens.size() == 5 || tokens.size() == 6,
                    "Wrong attribute definition: %s", line);
            NameImpl name = new NameImpl(tokens.get(0));
            Class<?> type;
            try {
                type = Class.forName(tokens.get(1));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Wrong type: " + tokens.get(1));
            }
            int min = Integer.parseInt(tokens.get(2));
            int max = Integer.parseInt(tokens.get(3));
            boolean nillable = Boolean.parseBoolean(tokens.get(4));

            /*
             * Default values that are currently not encoded.
             */
            boolean isIdentifiable = false;
            boolean isAbstract = false;
            List<Filter> restrictions = null;
            AttributeType superType = null;
            InternationalString description = null;
            Object defaultValue = null;

            AttributeType attributeType;
            AttributeDescriptor attributeDescriptor;
            if (Geometry.class.isAssignableFrom(type)) {
                String crsText = tokens.get(5);
                CoordinateReferenceSystem crs;
                boolean crsCode = crsText.startsWith("EPSG")
                        || crsText.startsWith("urn:ogc:def:crs:EPSG");
                try {
                    if (crsCode) {
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
                    throw new IllegalArgumentException("Cannot parse CRS definition: " + crsText);
                }

                attributeType = typeFactory.createGeometryType(name, type, crs, isIdentifiable,
                        isAbstract, restrictions, superType, description);
                attributeDescriptor = typeFactory.createGeometryDescriptor(
                        (GeometryType) attributeType, name, min, max, nillable, defaultValue);
            } else {
                attributeType = typeFactory.createAttributeType(name, type, isIdentifiable,
                        isAbstract, restrictions, superType, description);
                attributeDescriptor = typeFactory.createAttributeDescriptor(attributeType, name,
                        min, max, nillable, defaultValue);
            }
            return attributeDescriptor;
        }
    };

    /**
     * Tree reader.
     * <p>
     * Parses a tree in the format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * 
     * {@code "BUCKET" + "\t" +  <bucket_idx> + "\t" + <ObjectId> + "\n"}
     * or 
     * {@code "REF" + "\t" +  <ref_type> + "\t" + <ref_name> + "\t" + <ObjectId> + "\t" + <MetadataId> + "\t" + <bounds>"\n"}
     * .
     * .
     * .
     * </pre>
     * 
     */
    private static final TextReader<RevTree> TREE_READER = new TextReader<RevTree>() {

        @Override
        protected RevTree read(BufferedReader reader, TYPE type) throws IOException {
            Preconditions.checkArgument(TYPE.TREE.equals(type), "Wrong type: %s", type.name());
            Builder<Node> features = ImmutableList.builder();
            Builder<Node> trees = ImmutableList.builder();
            TreeMap<Integer, ObjectId> subtrees = Maps.newTreeMap();
            ObjectId id = ObjectId.valueOf(parseLine(reader.readLine(), "id"));
            long size = Long.parseLong(parseLine(reader.readLine(), "size"));
            String line;
            while ((line = reader.readLine()) != null) {
                Preconditions.checkArgument(!line.isEmpty(), "Empty tree element definition");
                ArrayList<String> tokens = Lists.newArrayList(Splitter.on('\t').split(line));
                String nodeType = tokens.get(0);
                if (nodeType.equals(TextWriter.TreeNode.REF.name())) {
                    Node entryRef = parseNodeLine(line);
                    if (entryRef.getType().equals(TYPE.TREE)) {
                        trees.add(entryRef);
                    } else {
                        features.add(entryRef);
                    }
                } else if (nodeType.equals(TextWriter.TreeNode.BUCKET.name())) {
                    Preconditions.checkArgument(tokens.size() == 3, "Wrong bucket definition: %s",
                            line);
                    Integer idx = Integer.parseInt(tokens.get(1));
                    ObjectId oid = ObjectId.valueOf(tokens.get(2));
                    subtrees.put(idx, oid);
                } else {
                    throw new IllegalArgumentException("Wrong tree element definition: " + line);
                }

            }

            RevTree tree;
            if (subtrees.isEmpty()) {
                tree = RevTreeImpl.createLeafTree(id, size, features.build(), trees.build());
            } else {
                tree = RevTreeImpl.createNodeTree(id, size, subtrees);
            }
            return tree;
        }

    };

}
