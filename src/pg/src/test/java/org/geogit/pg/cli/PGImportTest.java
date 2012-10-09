/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.pg.cli;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyMapOf;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.apache.commons.io.FileUtils;
import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.primitive.Point;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 *
 */
public class PGImportTest extends Assert {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private GeogitCLI cli;

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        File workingDirectory = new File("target", "repo");
        FileUtils.deleteDirectory(workingDirectory);
        assertTrue(workingDirectory.mkdir());
        Platform platform = new TestPlatform(workingDirectory);
        cli.setPlatform(platform);
        cli.execute("init");
        assertTrue(new File(workingDirectory, ".geogit").exists());
    }

    @Test
    public void testImport() throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geom", Point.class);
        builder.add("label", String.class);
        builder.setName("table1");
        SimpleFeatureType type = builder.buildFeatureType();

        GeometryFactory gf = new GeometryFactory();
        SimpleFeature f1 = SimpleFeatureBuilder.build(type,
                new Object[] { gf.createPoint(new Coordinate(5, 8)), "feature1" }, null);
        SimpleFeature f2 = SimpleFeatureBuilder.build(type,
                new Object[] { gf.createPoint(new Coordinate(5, 4)), "feature2" }, null);

        MemoryDataStore testDataStore = new MemoryDataStore();
        testDataStore.addFeature(f1);
        testDataStore.addFeature(f2);

        final AbstractDataStoreFactory factory = mock(AbstractDataStoreFactory.class);
        Map<String, Serializable> dataStoreParams = anyMapOf(String.class, Serializable.class);
        when(factory.createDataStore(dataStoreParams)).thenReturn(testDataStore);

        PGImport importCommand = new PGImport();
        importCommand.args.all = true;
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testNoTableNotAll() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.args.all = false;
        importCommand.args.table = "";
        exception.expect(Exception.class);
        importCommand.run(cli);
    }

    @Test
    public void testAllAndTable() throws Exception {
        PGImport importCommand = new PGImport();
        importCommand.args.all = true;
        importCommand.args.table = "table1";
        exception.expect(Exception.class);
        importCommand.run(cli);
    }

    @Test
    public void testNoRepository() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        PGImport importCommand = new PGImport();
        importCommand.args.all = true;
        exception.expect(Exception.class);
        importCommand.run(cli);
    }

    @Test
    public void testImportTable() throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geom", Point.class);
        builder.add("label", String.class);
        builder.setName("table1");
        SimpleFeatureType type = builder.buildFeatureType();

        SimpleFeatureTypeBuilder builder2 = new SimpleFeatureTypeBuilder();
        builder2.add("geom", Point.class);
        builder2.add("name", String.class);
        builder2.setName("table2");
        SimpleFeatureType type2 = builder2.buildFeatureType();

        GeometryFactory gf = new GeometryFactory();
        SimpleFeature f1 = SimpleFeatureBuilder.build(type,
                new Object[] { gf.createPoint(new Coordinate(5, 8)), "feature1" }, null);
        SimpleFeature f2 = SimpleFeatureBuilder.build(type,
                new Object[] { gf.createPoint(new Coordinate(5, 4)), "feature2" }, null);
        SimpleFeature f3 = SimpleFeatureBuilder.build(type2,
                new Object[] { gf.createPoint(new Coordinate(3, 2)), "feature3" }, null);

        MemoryDataStore testDataStore = new MemoryDataStore();
        testDataStore.addFeature(f1);
        testDataStore.addFeature(f2);
        testDataStore.addFeature(f3);

        final AbstractDataStoreFactory factory = mock(AbstractDataStoreFactory.class);
        Map<String, Serializable> dataStoreParams = anyMapOf(String.class, Serializable.class);
        when(factory.createDataStore(dataStoreParams)).thenReturn(testDataStore);

        PGImport importCommand = new PGImport();
        importCommand.args.all = false;
        importCommand.args.table = "table1";
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }
}
