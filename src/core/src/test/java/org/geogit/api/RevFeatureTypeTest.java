/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.ArrayList;

import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.collect.Lists;

public class RevFeatureTypeTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testConstructorAndAccessors() {
        RevFeatureType featureType = RevFeatureType.build(linesType);

        assertEquals(RevObject.TYPE.FEATURETYPE, featureType.getType());

        assertEquals(linesType, featureType.type());

        assertEquals(linesType.getName(), featureType.getName());

        ArrayList<PropertyDescriptor> descriptors = Lists.newArrayList(linesType.getDescriptors());
        // Collections.sort(descriptors, RevFeatureType.PROPERTY_ORDER);
        assertEquals(descriptors, featureType.sortedDescriptors());
    }

    @Test
    public void testToString() {
        RevFeatureType featureType = RevFeatureType.build(linesType);

        String featureTypeString = featureType.toString();

        assertEquals("FeatureType[" + featureType.getId().toString() + "; "
                + "sp: String, ip: Integer, pp: LineString]", featureTypeString);
    }
}
