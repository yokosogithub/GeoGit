/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api;

/**
 * Symbolic reference.
 */
public class SymRef extends Ref {

    private Ref target;

    /**
     * Constructs a new {@code SymRef} with the given name and target reference.
     * 
     * @param name the name of the symbolic reference
     * @param target the reference that this symbolic ref points to
     */
    public SymRef(String name, Ref target) {
        super(name, target.getObjectId(), target.getType());
        this.target = target;
    }

    /**
     * @return the reference that this symbolic ref points to
     */
    public String getTarget() {
        return target.getName();
    }

}
