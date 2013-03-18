package org.geogit.di;

import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.plumbing.merge.Conflict;
import org.geogit.api.plumbing.merge.ConflictsReadOp;

public class ConflictInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        AbstractGeoGitOp<?> operation = (AbstractGeoGitOp<?>) invocation.getThis();
        List<Conflict> conflicts = operation.command(ConflictsReadOp.class).call();
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Cannot run operation while merge conflicts exist.");
        }
        return invocation.proceed();
    }

}
