/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.storage.integration.mongo;

import org.geogit.api.Platform;

import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {
    private Platform testPlatform;

    /**
     * @param testPlatform
     */
    public TestModule(Platform testPlatform) {
        this.testPlatform = testPlatform;
    }

    @Override
    protected void configure() {
        if (testPlatform != null) {
            bind(Platform.class).toInstance(testPlatform);
        }
    }
}
