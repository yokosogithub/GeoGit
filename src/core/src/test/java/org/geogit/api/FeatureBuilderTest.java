package org.geogit.api;

import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;
import org.opengis.feature.Feature;

public class FeatureBuilderTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testFeatureBuilder() {
        FeatureBuilder builder = new FeatureBuilder(pointsType);
        RevFeatureBuilder revBuilder = new RevFeatureBuilder();
        RevFeature point1 = revBuilder.build(points1);

        Feature test = builder.build(idP1, point1);

        // assertEquals(points1.getValue(), test.getValue());
        assertEquals(points1.getName(), test.getName());
        assertEquals(points1.getIdentifier(), test.getIdentifier());
        assertEquals(points1.getType(), test.getType());
        assertEquals(points1.getUserData(), test.getUserData());
    }
}
