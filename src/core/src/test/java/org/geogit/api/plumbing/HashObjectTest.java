/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.geogit.api.CommandLocator;
import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTag;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.io.ParseException;

public class HashObjectTest extends RepositoryTestCase {

    private RevFeatureType featureType1;

    private RevFeatureType featureType2;

    private RevFeatureType featureType1Duplicate;

    private RevFeature pointFeature1;

    private RevFeature pointFeature2;

    private RevFeature pointFeature1Duplicate;

    private RevCommit commit1;

    private RevCommit commit2;

    private RevCommit commit1Duplicate;

    private HashObject hashCommand;

    private RevFeature coverageRevFeature;

    private RevFeatureType coverageRevFeatureType;

    private class SomeRandomClass {
        SomeRandomClass() {
        }
    }

    @Override
    protected void setUpInternal() throws Exception {
        featureType1 = RevFeatureType.build(pointsType);
        featureType2 = RevFeatureType.build(linesType);
        featureType1Duplicate = RevFeatureType.build(pointsType);

        RevFeatureBuilder featureBuilder = new RevFeatureBuilder();
        pointFeature1 = featureBuilder.build(points1);
        pointFeature2 = featureBuilder.build(points2);
        pointFeature1Duplicate = featureBuilder.build(points1);

        CommitBuilder b = new CommitBuilder();
        b.setAuthor("groldan");
        b.setAuthorEmail("groldan@opengeo.org");
        b.setCommitter("jdeolive");
        b.setCommitterEmail("jdeolive@opengeo.org");
        b.setMessage("cool this works");
        b.setCommitterTimestamp(1000);
        b.setCommitterTimeZoneOffset(5);

        ObjectId treeId = ObjectId.forString("fake tree content");

        b.setTreeId(treeId);

        ObjectId parentId1 = ObjectId.forString("fake parent content 1");
        ObjectId parentId2 = ObjectId.forString("fake parent content 2");
        List<ObjectId> parentIds = ImmutableList.of(parentId1, parentId2);
        b.setParentIds(parentIds);

        commit1 = b.build();
        commit1Duplicate = b.build();

        b.setMessage(null);
        b.setAuthor(null);
        b.setAuthorEmail(null);
        b.setCommitterTimestamp(-1000);
        b.setCommitterTimeZoneOffset(-5);
        b.setParentIds(ImmutableList.of(parentId1, ObjectId.NULL));

        commit2 = b.build();

        Object boolArray = new boolean[] { true, false, true, true, false };
        Object byteArray = new byte[] { 100, 127, -110, 26, 42 };
        Object charArray = new char[] { 'a', 'b', 'c', 'd', 'e' };
        Object doubleArray = new double[] { 1.5, 1.6, 1.7, 1.8 };
        Object floatArray = new float[] { 1.1f, 3.14f, 6.0f, 0.0f };
        Object intArray = new int[] { 5, 7, 9, 11, 32 };
        Object longArray = new long[] { 100, 200, 300, 400 };

        TestSerializableObject serializableObject = new TestSerializableObject();
        serializableObject.words = "words to serialize";

        SimpleFeatureType coverageFeatureType = DataUtilities.createType(
                "http://geoserver.org/test", "TestType", "str:String," + "str2:String,"
                        + "bool:Boolean," + "byte:java.lang.Byte," + "doub:Double,"
                        + "bdec:java.math.BigDecimal," + "flt:Float," + "int:Integer,"
                        + "bint:java.math.BigInteger," + "boolArray:java.lang.Object,"
                        + "byteArray:java.lang.Object," + "charArray:java.lang.Object,"
                        + "doubleArray:java.lang.Object," + "floatArray:java.lang.Object,"
                        + "intArray:java.lang.Object," + "longArray:java.lang.Object,"
                        + "serialized:java.io.Serializable," + "randomClass:java.lang.Object,"
                        + "pp:Point:srid=4326," + "lng:java.lang.Long," + "uuid:java.util.UUID");

        coverageRevFeatureType = RevFeatureType.build(coverageFeatureType);

        Feature coverageFeature = feature(coverageFeatureType, "TestType.Coverage.1",
                "StringProp1_1", null, Boolean.TRUE, Byte.valueOf("18"), new Double(100.01),
                new BigDecimal("1.89e1021"), new Float(12.5), new Integer(1000), new BigInteger(
                        "90000000"), boolArray, byteArray, charArray, doubleArray, floatArray,
                intArray, longArray, serializableObject, new SomeRandomClass(), "POINT(1 1)",
                new Long(800000), UUID.fromString("bd882d24-0fe9-11e1-a736-03b3c0d0d06d"));

        coverageRevFeature = featureBuilder.build(coverageFeature);

        hashCommand = new HashObject();

        CommandLocator mockCommandLocator = mock(CommandLocator.class);
        hashCommand.setCommandLocator(mockCommandLocator);
        when(mockCommandLocator.command(eq(DescribeFeatureType.class))).thenReturn(
                new DescribeFeatureType());
    }

    @Test
    public void testHashNullObject() throws Exception {
        try {
            hashCommand.setObject(null).call();
            fail("expected IllegalStateException on null feature type");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Object has not been set"));
        }
    }

    @Test
    public void testHashFeatures() throws Exception {

        ObjectId feature1Id = hashCommand.setObject(pointFeature1).call();
        ObjectId feature2Id = hashCommand.setObject(pointFeature2).call();
        ObjectId coverageFeatureId = hashCommand.setObject(coverageRevFeature).call();
        ObjectId feature1DuplicateId = hashCommand.setObject(pointFeature1Duplicate).call();

        assertNotNull(feature1Id);
        assertNotNull(feature2Id);
        assertNotNull(coverageFeatureId);
        assertNotNull(feature1DuplicateId);

        assertFalse(feature1Id.equals(feature2Id));
        assertFalse(feature1Id.equals(coverageFeatureId));
        assertFalse(feature2Id.equals(coverageFeatureId));
        assertTrue(feature1Id.equals(feature1DuplicateId));

    }

    @Test
    public void testHashFeatureTypes() throws Exception {

        ObjectId featureType1Id = hashCommand.setObject(featureType1).call();
        ObjectId featureType2Id = hashCommand.setObject(featureType2).call();
        ObjectId coverageTypeId = hashCommand.setObject(coverageRevFeatureType).call();
        ObjectId featureType1DuplicateId = hashCommand.setObject(featureType1Duplicate).call();

        assertNotNull(featureType1Id);
        assertNotNull(featureType2Id);
        assertNotNull(coverageTypeId);
        assertNotNull(featureType1DuplicateId);

        assertFalse(featureType1Id.equals(featureType2Id));
        assertFalse(featureType1Id.equals(coverageTypeId));
        assertFalse(featureType2Id.equals(coverageTypeId));
        assertTrue(featureType1Id.equals(featureType1DuplicateId));

    }

    @Test
    public void testHashCommits() throws Exception {

        ObjectId commit1Id = hashCommand.setObject(commit1).call();
        ObjectId commit2Id = hashCommand.setObject(commit2).call();
        ObjectId commit1DuplicateId = hashCommand.setObject(commit1Duplicate).call();

        assertNotNull(commit1Id);
        assertNotNull(commit2Id);
        assertNotNull(commit1DuplicateId);

        assertFalse(commit1Id.equals(commit2Id));
        assertTrue(commit1Id.equals(commit1DuplicateId));

    }

    @Test
    public void testHashTags() throws Exception {

        RevPerson tagger = new RevPerson("volaya", "volaya@opengeo.org", -1000, -1);
        RevPerson tagger2 = new RevPerson("groldan", "groldan@opengeo.org", 10000, 0);
        RevTag tag = new RevTag(null, "tag1", ObjectId.forString("fake commit id"), "message",
                tagger);
        RevTag tag2 = new RevTag(null, "tag2", ObjectId.forString("another fake commit id"),
                "another message", tagger2);
        ObjectId tagId = hashCommand.setObject(tag).call();
        ObjectId tagId2 = hashCommand.setObject(tag2).call();
        assertNotNull(tagId);
        assertNotNull(tagId2);
        assertNotSame(tagId, tagId2);

    }

    protected Feature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }
}
