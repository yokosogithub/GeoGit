/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevObject.TYPE;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.io.ParseException;

public abstract class RevFeatureSerializationTest extends Assert {

    private ObjectSerialisingFactory factory;

    private String namespace1 = "http://geoserver.org/test";

    private String typeName1 = "TestType";

    private String typeSpec1 = "str:String," + "bool:Boolean," + "byte:java.lang.Byte,"
            + "doub:Double," + "bdec:java.math.BigDecimal," + "flt:Float," + "int:Integer,"
            + "bint:java.math.BigInteger," + "pp:Point:srid=4326," + "lng:java.lang.Long,"
            + "uuid:java.util.UUID";

    protected SimpleFeatureType featureType1;

    protected Feature testFeature1;

    private Feature testFeatureWithNulls1;

    private Feature testFeatureWithMultilineString;

    @Before
    public void before() throws Exception {
        this.factory = getFactory();
        featureType1 = DataUtilities.createType(namespace1, typeName1, typeSpec1);
        testFeature1 = feature(featureType1, "TestType.feature.1", "StringProp1_1", Boolean.TRUE,
                Byte.valueOf("18"), new Double(100.01), new BigDecimal("1.89e1021"),
                new Float(12.5), new Integer(1000), new BigInteger("90000000"), "POINT(1 1)",
                new Long(800000), UUID.fromString("bd882d24-0fe9-11e1-a736-03b3c0d0d06d"));
        testFeatureWithMultilineString = feature(featureType1, "TestType.feature.1",
                "This\nis\na\nmultiline\nstring", Boolean.TRUE, Byte.valueOf("18"), new Double(
                        100.01), new BigDecimal("1.89e1021"), new Float(12.5), new Integer(1000),
                new BigInteger("90000000"), "POINT(1 1)", new Long(800000),
                UUID.fromString("bd882d24-0fe9-11e1-a736-03b3c0d0d06d"));
        testFeatureWithNulls1 = feature(featureType1, "TestType.feature.1", null, null, null, null,
                null, null, null, null, null, null, null);
    }

    protected abstract ObjectSerialisingFactory getFactory();

    @Test
    public void testCommitSerialization() throws IOException {
        RevFeature revFeature = new RevFeatureBuilder().build(testFeature1);
        testFeatureReadWrite(revFeature);
        revFeature = new RevFeatureBuilder().build(testFeatureWithNulls1);
        testFeatureReadWrite(revFeature);
        revFeature = new RevFeatureBuilder().build(testFeatureWithMultilineString);
        testFeatureReadWrite(revFeature);
    }

    protected void testFeatureReadWrite(RevFeature feature) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectWriter<RevFeature> writer = factory.createObjectWriter(TYPE.FEATURE);
        writer.write(feature, out);
        ObjectReader<RevFeature> reader = factory.createFeatureReader();
        RevFeature read = reader.read(feature.getId(), new ByteArrayInputStream(out.toByteArray()));
        assertEquals(feature, read);
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
