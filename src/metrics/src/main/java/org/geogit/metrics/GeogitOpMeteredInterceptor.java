/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.metrics;

import org.aopalliance.intercept.MethodInvocation;
import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase;

import com.google.inject.Provider;

/**
 * Intercepts calls to {@link AbstractGeoGitOp#call()} implementations and logs its time
 * 
 * @see MetricsModule
 */
class GeogitOpMeteredInterceptor extends MethodMeteredInterceptor {

    public GeogitOpMeteredInterceptor(Provider<Platform> platform, Provider<ConfigDatabase> configDb) {
        super(platform, configDb);
    }

    /**
     * Being an interceptor for all command {@code call()} methods, resolves to the command name
     * (e.g. "ImportOp", "CommitOp", etc).
     */
    @Override
    protected String getName(MethodInvocation invocation) {
        // String name = invocation.getMethod().getAnnotation(Timed.class).name();
        String name = invocation.getThis().getClass().getSimpleName();
        final int idx = name.indexOf('$');// prefix of $$EnhancerByGuice$$xxxx. Could be the prefix
                                          // of an inner class but as a norm we write commands as
                                          // top level classes
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        return name;
    }

}
