/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.test.performance;

import org.geogit.di.GeogitModule;
import org.geogit.test.integration.je.JETestStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class JELogOpPerformanceTest extends LogOpPerformanceTest {
    @Override
    protected Injector createInjector() {
        return Guice.createInjector(Modules.override(new GeogitModule()).with(
                new JETestStorageModule()));
    }
}
