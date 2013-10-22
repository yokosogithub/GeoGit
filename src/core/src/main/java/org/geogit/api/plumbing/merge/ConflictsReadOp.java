/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;

import java.util.List;

import org.geogit.api.AbstractGeoGitOp;

public class ConflictsReadOp extends AbstractGeoGitOp<List<Conflict>> {

    @Override
    public List<Conflict> call() {
        return getIndex().getDatabase().getConflicts(null, null);
    }

}
