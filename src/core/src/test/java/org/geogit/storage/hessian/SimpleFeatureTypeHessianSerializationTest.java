package org.geogit.storage.hessian;

import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.storage.RevFeatureTypeSerializationTest;

public class SimpleFeatureTypeHessianSerializationTest extends RevFeatureTypeSerializationTest {

    @Override
    protected ObjectSerialisingFactory getFactory() {
        return new HessianFactory();
    }

}
