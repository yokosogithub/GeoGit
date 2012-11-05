package org.geogit.geotools.plubming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.geogit.geotools.plumbing.DescribeOp;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.porcelain.TestHelper;
import org.geotools.data.memory.MemoryDataStore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

public class DescribeOpTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testAccessorsAndMutators() throws Exception {
        DescribeOp describe = new DescribeOp();

        MemoryDataStore testDataStore = new MemoryDataStore();
        describe.setDataStore(testDataStore);
        assertEquals(testDataStore, describe.getDataStore());

        describe.setTable("table1");
        assertEquals("table1", describe.getTable());

    }

    @Test
    public void testNullDataStore() throws Exception {
        DescribeOp describe = new DescribeOp();
        describe.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        describe.call();
    }

    @Test
    public void testNullTable() throws Exception {
        DescribeOp describe = new DescribeOp();
        describe.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        exception.expect(GeoToolsOpException.class);
        describe.call();
    }

    @Test
    public void testEmptyTable() throws Exception {
        DescribeOp describe = new DescribeOp();
        describe.setTable("");
        describe.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        exception.expect(GeoToolsOpException.class);
        describe.call();
    }

    @Test
    public void testEmptyDataStore() throws Exception {
        DescribeOp describe = new DescribeOp();
        describe.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        describe.setTable("table1");
        Optional<Map<String, String>> features = describe.call();
        assertFalse(features.isPresent());
    }

    @Test
    public void testTypeNameException() throws Exception {
        DescribeOp describe = new DescribeOp();
        describe.setDataStore(TestHelper.createFactoryWithGetNamesException().createDataStore(null));
        describe.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        describe.call();
    }

    @Test
    public void testGetFeatureSourceException() throws Exception {
        DescribeOp describe = new DescribeOp();
        describe.setDataStore(TestHelper.createFactoryWithGetFeatureSourceException()
                .createDataStore(null));
        describe.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        describe.call();
    }

    @Test
    public void testDescribe() throws Exception {
        DescribeOp describe = new DescribeOp();
        describe.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        describe.setTable("table1");
        Optional<Map<String, String>> properties = describe.call();
        assertTrue(properties.isPresent());

        assertEquals("Point", properties.get().get("geom"));
        assertEquals("String", properties.get().get("label"));
    }
}
