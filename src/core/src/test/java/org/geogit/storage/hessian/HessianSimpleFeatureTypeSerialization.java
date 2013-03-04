package org.geogit.storage.hessian;

import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RevFeatureTypeSerialisationTest;

public class HessianSimpleFeatureTypeSerialization extends RevFeatureTypeSerialisationTest {
    @Override
    protected ObjectSerialisingFactory getObjectSerialisingFactory() {
        return new HessianFactory();
    }
}
