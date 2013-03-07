/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage;

import java.util.Iterator;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.repository.Repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Provider;

/**
 * Method interceptor for {@link ObjectDatabase#put(RevObject)} that adds new commits to the graph
 * database.
 */
public class ObjectDatabasePutInterceptor implements MethodInterceptor {

    private Provider<GraphDatabase> graphDb;

    private Provider<Repository> repository;

    public ObjectDatabasePutInterceptor(Provider<GraphDatabase> graphDb,
            Provider<Repository> repository) {
        this.graphDb = graphDb;
        this.repository = repository;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("put")) {
            if (invocation.getArguments()[0].getClass().equals(ObjectId.class)) {
                return putObjectIdInputStreamInterceptor(invocation);
            } else {
                return putRevObjectInterceptor(invocation);
            }

        } else if (invocation.getMethod().getName().equals("putAll")) {
            return putAllInterceptor(invocation);
        }
        return invocation.proceed();
    }

    private Object putAllInterceptor(MethodInvocation invocation) throws Throwable {
        @SuppressWarnings("unchecked")
        final Iterator<? extends RevObject> objects = (Iterator<? extends RevObject>) invocation
                .getArguments()[0];
        List<? extends RevObject> list = Lists.newArrayList(objects);
        for (RevObject o : list) {
            if (o.getType() == RevObject.TYPE.COMMIT) {
                // add to graph database
                RevCommit commit = (RevCommit) o;
                graphDb.get().put(commit.getId(), ImmutableList.copyOf(commit.getParentIds()));
            }
        }
        invocation.getArguments()[0] = list.iterator();

        return invocation.proceed();
    }

    private Object putObjectIdInputStreamInterceptor(MethodInvocation invocation) throws Throwable {
        final ObjectId objectId = (ObjectId) invocation.getArguments()[0];
        Object result = invocation.proceed();

        if (repository.get().commitExists(objectId)) {
            RevCommit commit = repository.get().getCommit(objectId);
            graphDb.get().put(commit.getId(), ImmutableList.copyOf(commit.getParentIds()));
        }

        return result;
    }

    private Object putRevObjectInterceptor(MethodInvocation invocation) throws Throwable {
        final RevObject revObject = (RevObject) invocation.getArguments()[0];

        if (revObject.getType() == RevObject.TYPE.COMMIT) {
            // add to graph database
            RevCommit commit = (RevCommit) revObject;
            graphDb.get().put(commit.getId(), ImmutableList.copyOf(commit.getParentIds()));
        }

        return invocation.proceed();
    }
}
