/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

/**
 * Method interceptor for {@link ObjectDatabase#put(RevObject)} that adds new commits to the graph
 * database.
 */
public class ObjectDatabasePutInterceptor implements MethodInterceptor {

    private GraphDatabase graphDb;

    @Inject
    ObjectDatabasePutInterceptor(GraphDatabase graphDb) {
        this.graphDb = graphDb;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final RevObject revObject = (RevObject) invocation.getArguments()[0];

        if (revObject.getType() == RevObject.TYPE.COMMIT) {
            // add to graph database
            RevCommit commit = (RevCommit) revObject;
            graphDb.put(commit.getId(), ImmutableList.copyOf(commit.getParentIds()));
        }

        return invocation.proceed();
    }
}
