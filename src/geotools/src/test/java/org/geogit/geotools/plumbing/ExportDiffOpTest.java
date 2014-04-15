/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.plumbing;

import java.util.List;

import org.geogit.api.RevCommit;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.google.common.base.Objects;

public class ExportDiffOpTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
    }

    @Test
    public void testExportDiff() throws Exception {
        insertAndAdd(points1);
        final RevCommit insertCommit = geogit.command(CommitOp.class).setAll(true).call();

        final String featureId = points1.getIdentifier().getID();
        final Feature modifiedFeature = feature((SimpleFeatureType) points1.getType(), featureId,
                "changedProp", new Integer(1500));
        insertAndAdd(modifiedFeature, points2);
        final RevCommit changeCommit = geogit.command(CommitOp.class).setAll(true).call();

        Feature[] points = new Feature[] { modifiedFeature, points2 };

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geogit_fid", String.class);
        for (AttributeDescriptor descriptor : pointsType.getAttributeDescriptors()) {
            builder.add(descriptor);
        }
        builder.setName(pointsType.getName());
        builder.setCRS(pointsType.getCoordinateReferenceSystem());
        SimpleFeatureType outputFeatureType = builder.buildFeatureType();
        MemoryDataStore dataStore = new MemoryDataStore(outputFeatureType);
        final String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        geogit.command(ExportDiffOp.class).setFeatureStore(featureStore).setPath(pointsName)
                .setNewRef(changeCommit.getId().toString())
                .setOldRef(insertCommit.getId().toString()).call();
        featureSource = dataStore.getFeatureSource(typeName);
        featureStore = (SimpleFeatureStore) featureSource;
        SimpleFeatureCollection featureCollection = featureStore.getFeatures();
        assertEquals(featureCollection.size(), points.length);
        SimpleFeatureIterator features = featureCollection.features();
        assertTrue(collectionsAreEqual(features, points));
    }

    @Test
    public void testExportDiffUsingOldVersion() throws Exception {
        insertAndAdd(points1);
        final RevCommit insertCommit = geogit.command(CommitOp.class).setAll(true).call();

        final String featureId = points1.getIdentifier().getID();
        final Feature modifiedFeature = feature((SimpleFeatureType) points1.getType(), featureId,
                "changedProp", new Integer(1500));
        insertAndAdd(modifiedFeature, points2);
        final RevCommit changeCommit = geogit.command(CommitOp.class).setAll(true).call();

        Feature[] points = new Feature[] { points1 };
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geogit_fid", String.class);
        for (AttributeDescriptor descriptor : pointsType.getAttributeDescriptors()) {
            builder.add(descriptor);
        }
        builder.setName(pointsType.getName());
        builder.setCRS(pointsType.getCoordinateReferenceSystem());
        SimpleFeatureType outputFeatureType = builder.buildFeatureType();
        MemoryDataStore dataStore = new MemoryDataStore(outputFeatureType);
        final String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        geogit.command(ExportDiffOp.class).setFeatureStore(featureStore).setPath(pointsName)
                .setNewRef(changeCommit.getId().toString())
                .setOldRef(insertCommit.getId().toString()).setUseOld(true).call();
        featureSource = dataStore.getFeatureSource(typeName);
        featureStore = (SimpleFeatureStore) featureSource;
        SimpleFeatureCollection featureCollection = featureStore.getFeatures();
        assertEquals(featureCollection.size(), points.length);
        SimpleFeatureIterator features = featureCollection.features();
        assertTrue(collectionsAreEqual(features, points));
    }

    private boolean collectionsAreEqual(SimpleFeatureIterator features, Feature[] points) {
        // features are not iterated in the same order as the original set, so
        // we just do pairwise comparison to check that all the original features
        // are represented in the exported feature store
        while (features.hasNext()) {
            boolean found = true;
            List<Object> attributesExported = features.next().getAttributes();
            for (int i = 0; i < points.length; i++) {
                found = true;
                List<Object> attributesOriginal = ((SimpleFeature) points[i]).getAttributes();
                for (int j = 0; j < attributesExported.size() - 1; j++) {
                    Object attributeExported = attributesExported.get(j + 1);
                    Object attributeOriginal = attributesOriginal.get(j);
                    if (!Objects.equal(attributeOriginal, attributeExported)) {
                        found = false;
                        break;
                    }
                }
                found = found
                        && points[i].getIdentifier().getID().equals(attributesExported.get(0));
                if (found) {
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
