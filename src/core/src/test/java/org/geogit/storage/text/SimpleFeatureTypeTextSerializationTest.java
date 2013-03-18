package org.geogit.storage.text;

import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.RevFeatureTypeSerializationTest;

public class SimpleFeatureTypeTextSerializationTest extends RevFeatureTypeSerializationTest {

    @Override
    protected ObjectSerializingFactory getObjectSerializingFactory() {
        return new TextSerializationFactory();
    }
}
