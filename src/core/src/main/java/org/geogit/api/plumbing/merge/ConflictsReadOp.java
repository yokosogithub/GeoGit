/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing.merge;


import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.plumbing.ResolveGeogitDir;
import com.google.common.base.Preconditions;

public class ConflictsReadOp extends AbstractGeoGitOp<List<Conflict>> {
    @Override
    public List<Conflict> call() {
        final URL repoUrl = getCommandLocator().command(ResolveGeogitDir.class).call();
        if (repoUrl == null) {
            return Collections.emptyList();
        } else {
            getIndex().getDatabase().open();
            return getIndex().getDatabase().getConflicts(null, null);
        }
    }
}
