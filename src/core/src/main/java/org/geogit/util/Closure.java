/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.util;

public interface Closure<T> {

    /**
     * Performs an action on the specified input object.
     */
    public void execute(T input);

}
