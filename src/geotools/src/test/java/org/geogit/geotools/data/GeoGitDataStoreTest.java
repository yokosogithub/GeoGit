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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

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
        assertTypeRefs(Collections.singleton(super.linesType));

        dataStore.createSchema(super.pointsType);

        Set<SimpleFeatureType> expected = new HashSet<SimpleFeatureType>();
        expected.add(super.linesType);
        expected.add(super.pointsType);
        assertTypeRefs(expected);

        try {
            dataStore.createSchema(super.pointsType);
            fail("Expected IOException on existing type");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("already exists"));
        }

    }

    private void assertTypeRefs(Set<SimpleFeatureType> expectedTypes) throws IOException {
        //
        // for (SimpleFeatureType featureType : expectedTypes) {
        // final Name typeName = featureType.getName();
        // final Ref typesTreeRef = geogit.command(RefParse.class)
        // .setName(GeoGitDataStore.TYPE_NAMES_REF_TREE).call();
        // assertNotNull(typesTreeRef);
        //
        // RevTree typesTree = repo.getTree(typesTreeRef.getObjectId());
        // String path = typeName.getLocalPart();
        // ObjectDatabase objectDatabase = repo.getObjectDatabase();
        //
        // NodeRef typeRef = objectDatabase.getTreeChild(typesTree, path);
        // assertNotNull(typeRef);
        // assertEquals(TYPE.FEATURE, typeRef.getType());
        //
        // SimpleFeatureType readType = (SimpleFeatureType) objectDatabase.get(
        // typeRef.getObjectId(), getRepository().newFeatureTypeReader()).type();
        //
        // assertEquals(featureType, readType);
        //
        // }
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
