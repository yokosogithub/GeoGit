/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

import com.google.common.base.Optional;

public interface MutableTree extends RevTree {

    public abstract void put(final NodeRef ref);

    public abstract Optional<NodeRef> remove(final String key);

    public abstract void normalize();

}
