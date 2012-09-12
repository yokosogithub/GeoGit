/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 *
 */
public class SymRef extends Ref {

    private Ref target;

    public SymRef(String name, Ref target) {
        super(name, target.getObjectId(), target.getType());
        this.target = target;
    }

    public String getTarget() {
        return target.getName();
    }

}
