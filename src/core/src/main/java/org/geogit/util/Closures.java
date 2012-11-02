/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.util;

/**
 *
 */
public class Closures {

    public static <T> Closure<T> doNothing() {
        return new Closure<T>() {
            @Override
            public void execute(T input) {
                // do nothing
            }
        };
    }
}
