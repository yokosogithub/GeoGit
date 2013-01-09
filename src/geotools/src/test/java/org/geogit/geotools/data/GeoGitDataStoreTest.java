/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.geotools.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geogit.api.NodeRef;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class GeoGitDataStoreTest extends RepositoryTestCase {

    private GeoGitDataStore dataStore;

    @Override
    protected void setUpInternal() throws Exception {
        dataStore = new GeoGitDataStore(geogit);
    }

    @Override
    protected void tearDownInternal() throws Exception {
        dataStore = null;
    }

    @Test
    public void testCreateSchema() throws IOException {

        final SimpleFeatureType featureType = super.linesType;
        dataStore.createSchema(featureType);

        List<NodeRef> typeTrees = geogit.getRepository().getWorkingTree().getFeatureTypeTrees();
        assertEquals(1, typeTrees.size());
        assertEquals(linesName, typeTrees.get(0).name());

        dataStore.createSchema(super.pointsType);

        typeTrees = geogit.getRepository().getWorkingTree().getFeatureTypeTrees();
        assertEquals(2, typeTrees.size());
        List<String> typeNames = Lists.newArrayList(Iterators.transform(typeTrees.iterator(),
                new Function<NodeRef, String>() {

                    @Override
                    public String apply(NodeRef input) {
                        return input.name();
                    }
                }));

        assertTrue(typeNames.toString(), typeNames.contains(linesName));
        assertTrue(typeNames.toString(), typeNames.contains(pointsName));

        try {
            dataStore.createSchema(super.pointsType);
            fail("Expected IOException on existing type");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("already exists"));
        }

    }

    @Test
    public void testGetNames() throws Exception {

        assertEquals(0, dataStore.getNames().size());

        insert(points1);
        assertEquals(1, dataStore.getNames().size());

        insert(lines1);
        assertEquals(2, dataStore.getNames().size());

        List<Name> names = dataStore.getNames();
        // ContentDataStore doesn't support native namespaces
        // assertTrue(names.contains(RepositoryTestCase.linesTypeName));
        // assertTrue(names.contains(RepositoryTestCase.pointsTypeName));
        assertTrue(names.contains(new NameImpl(pointsName)));
        assertTrue(names.contains(new NameImpl(linesName)));
    }

    @Test
    public void testGetTypeNames() throws Exception {

        assertEquals(0, dataStore.getTypeNames().length);

        insert(lines1);
        assertEquals(1, dataStore.getTypeNames().length);

        insert(points1);
        assertEquals(2, dataStore.getTypeNames().length);

        List<String> simpleNames = Arrays.asList(dataStore.getTypeNames());

        assertTrue(simpleNames.contains(linesName));
        assertTrue(simpleNames.contains(pointsName));
    }

    @Test
    public void testGetSchemaName() throws Exception {
        try {
            dataStore.getSchema(RepositoryTestCase.linesTypeName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("does not exist"));
        }

        insert(lines1);
        SimpleFeatureType lines = dataStore.getSchema(RepositoryTestCase.linesTypeName);
        assertEquals(super.linesType, lines);

        try {
            dataStore.getSchema(RepositoryTestCase.pointsTypeName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(true);
        }

        insert(points1);
        SimpleFeatureType points = dataStore.getSchema(RepositoryTestCase.pointsTypeName);
        assertEquals(super.pointsType, points);
    }

    @Test
    public void testGetSchemaString() throws Exception {
        try {
            dataStore.getSchema(RepositoryTestCase.linesName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(true);
        }

        insert(lines1);
        SimpleFeatureType lines = dataStore.getSchema(RepositoryTestCase.linesName);
        assertEquals(super.linesType, lines);

        try {
            dataStore.getSchema(RepositoryTestCase.pointsName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(true);
        }

        insert(points1);
        SimpleFeatureType points = dataStore.getSchema(RepositoryTestCase.pointsName);
        assertEquals(super.pointsType, points);
    }

    @Test
    public void testGetFeatureSourceName() throws Exception {
        try {
            dataStore.getFeatureSource(RepositoryTestCase.linesTypeName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(true);
        }

        SimpleFeatureSource source;

        insert(lines1);
        source = dataStore.getFeatureSource(RepositoryTestCase.linesTypeName);
        assertTrue(source instanceof GeogitFeatureSource);

        try {
            dataStore.getFeatureSource(RepositoryTestCase.pointsTypeName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(true);
        }

        insert(points1);
        source = dataStore.getFeatureSource(RepositoryTestCase.pointsTypeName);
        assertTrue(source instanceof GeogitFeatureSource);
    }

    @Test
    public void testGetFeatureSourceString() throws Exception {
        try {
            dataStore.getFeatureSource(RepositoryTestCase.linesName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(true);
        }

        SimpleFeatureSource source;

        insert(lines1);
        source = dataStore.getFeatureSource(RepositoryTestCase.linesName);
        assertTrue(source instanceof GeogitFeatureSource);

        try {
            dataStore.getFeatureSource(RepositoryTestCase.pointsName);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(true);
        }

        insert(points1);
        source = dataStore.getFeatureSource(RepositoryTestCase.pointsName);
        assertTrue(source instanceof GeogitFeatureSource);
    }

}
