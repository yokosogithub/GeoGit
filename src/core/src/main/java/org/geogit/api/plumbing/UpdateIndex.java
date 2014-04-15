/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.porcelain.AddOp;

/**
 * Register file contents in the working tree to the index.
 * <p>
 * Modifies the index or staging area. Each feature mentioned is updated into the index and any
 * unmerged or needs updating state is cleared.
 * 
 * See also {@link AddOp} for a more user-friendly way to do some of the most common operations on
 * the index.
 * 
 * The way geogit update-index handles features it is told about can be modified using the various
 * options:
 * <ul>
 * <li>{@code add}: If a specified feature isn’t in the index already then it’s added. Default
 * behavior is to ignore new features.
 * <li>{@code remove}: If a specified feature is in the index but is missing then it’s removed.
 * Default behavior is to ignore removed features.
 * <li>{@code refresh}: Looks at the current index and checks to see if merges or updates are
 * needed.
 * <li>{@code unmerged}: If {@code refresh == true} and finds unmerged changes in the index, the
 * default behavior is to error out. This option makes geogit update-index continue anyway.
 * <li> {@code ignoremissing}: Ignores missing features during a {@code refresh}
 * <li>cacheinfo <mode> <object> <path> Directly insert the specified info into the index.
 * </ul>
 */
public class UpdateIndex extends AbstractGeoGitOp<Void> {

    @Override
    public Void call() {
        throw new UnsupportedOperationException("not yet implemented");
    }

}
