package org.geogit.storage.datastream;

import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.RevFeatureTypeSerializationTest;

public class DataStreamFeatureTypeSerialization extends RevFeatureTypeSerializationTest {
    @Override
    protected ObjectSerializingFactory getObjectSerializingFactory() {
        return new DataStreamSerializationFactory();
    }
}
