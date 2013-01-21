package org.geogit.storage.text;

import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RevFeatureTypeSerializationTest;

public class SimpleFeatureTypeTextSerializationTest extends RevFeatureTypeSerializationTest {

    ObjectSerialisingFactory factory = new TextSerializationFactory();

    @Override
    protected ObjectSerialisingFactory getFactory() {
        return factory;
    }
}
