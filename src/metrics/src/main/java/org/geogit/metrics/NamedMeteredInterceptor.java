/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.metrics;

import org.aopalliance.intercept.MethodInvocation;
import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;

import com.google.inject.Provider;

/**
 * Intercepts calls to a designated method and logs its timing, with the provided name.
 * 
 * @see MetricsModule
 */
class NamedMeteredInterceptor extends MethodMeteredInterceptor {

    private String logMethodName;

    public NamedMeteredInterceptor(Provider<Platform> platform, Provider<ConfigDatabase> configDb,
            String logMethodName) {
        super(platform, configDb);
        this.logMethodName = logMethodName;
    }

    /**
     * @return the provided name for the log events
     */
    @Override
    protected String getName(MethodInvocation invocation) {
        return logMethodName;
    }
}
