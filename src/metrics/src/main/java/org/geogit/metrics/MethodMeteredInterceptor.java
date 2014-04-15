/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.metrics;

import static org.geogit.metrics.MetricsModule.COMMAND_STACK_LOGGER;
import static org.geogit.metrics.MetricsModule.METRICS_ENABLED;
import static org.geogit.metrics.MetricsModule.METRICS_LOGGER;

import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geogit.api.Platform;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigException.StatusCode;
import org.geogit.storage.ConfigDatabase;

import com.google.inject.Provider;

/**
 * 
 * @see MetricsModule
 */
abstract class MethodMeteredInterceptor implements MethodInterceptor {

    private static final double toMillisFactor = 1.0 / TimeUnit.MILLISECONDS.toNanos(1L);

    private Provider<Platform> platform;

    private Provider<ConfigDatabase> configDb;

    public MethodMeteredInterceptor(Provider<Platform> platform, Provider<ConfigDatabase> configDb) {
        this.platform = platform;
        this.configDb = configDb;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Boolean enabled;
        try {
            enabled = configDb.get().get(METRICS_ENABLED, Boolean.class).or(Boolean.FALSE);
        } catch (ConfigException e) {
            if (StatusCode.INVALID_LOCATION.equals(e.statusCode)) {
                enabled = Boolean.FALSE;
            } else {
                throw e;
            }
        }
        if (!enabled.booleanValue()) {
            return invocation.proceed();
        }

        final Platform platform = this.platform.get();

        String name = getName(invocation);
        boolean success = true;

        CallStack stack = CallStack.push(name);
        final long startTime = platform.currentTimeMillis();
        long nanoTime = platform.nanoTime();
        try {
            return invocation.proceed();
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            nanoTime = platform.nanoTime() - nanoTime;
            double millis = nanoTime * toMillisFactor;
            METRICS_LOGGER.info("{}, {}, {}, {}", name, startTime, millis, success);
            stack = CallStack.pop(nanoTime, success);
            if (stack.isRoot()) {
                COMMAND_STACK_LOGGER.info("{}", stack.toString(TimeUnit.MILLISECONDS));
            }
        }
    }

    /**
     * Called by {@link #invoke(MethodInvocation)} to resolve the name by which the logging events
     * will be created
     */
    protected abstract String getName(MethodInvocation invocation);
}
