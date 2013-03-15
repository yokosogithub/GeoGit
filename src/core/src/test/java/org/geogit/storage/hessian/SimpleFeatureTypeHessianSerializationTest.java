package org.geogit.storage.hessian;

import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.RevFeatureTypeSerializationTest;

public class SimpleFeatureTypeHessianSerializationTest extends RevFeatureTypeSerializationTest {

    @Override
    protected ObjectSerializingFactory getObjectSerializingFactory() {
        return new HessianFactory();
    }

}
