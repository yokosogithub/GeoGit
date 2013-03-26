package org.geogit.storage.text;

import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.RevFeatureTypeSerializationTest;

public class RevFeatureTypeTextSerializationTest extends RevFeatureTypeSerializationTest {

    @Override
    protected ObjectSerializingFactory getObjectSerializingFactory() {
        return new TextSerializationFactory();
    }
}
