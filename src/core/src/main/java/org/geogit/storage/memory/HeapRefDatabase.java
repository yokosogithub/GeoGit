/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.storage.RefDatabase;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 *
 */
public class HeapRefDatabase implements RefDatabase {

    private Map<String, String> refs;

    /**
     * 
     * @see org.geogit.storage.RefDatabase#create()
     */
    @Override
    public void create() {
        if (refs == null) {
            refs = Maps.newTreeMap();
        }
    }

    /**
     * 
     * @see org.geogit.storage.RefDatabase#close()
     */
    @Override
    public void close() {
        if (refs != null) {
            refs.clear();
            refs = null;
        }
    }

    /**
     * @param name
     * @return
     * @see org.geogit.storage.RefDatabase#getRef(java.lang.String)
     */
    @Override
    public String getRef(String name) {
        String val = refs.get(name);
        if (val == null) {
            return null;
        }
        try {
            ObjectId.valueOf(val);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        return val;
    }

    /**
     * @param ref
     * @return
     * @see org.geogit.storage.RefDatabase#put(org.geogit.api.Ref)
     */
    @Override
    public String putRef(String name, String value) {
        checkNotNull(name);
        checkNotNull(value);
        ObjectId.valueOf(value);
        return refs.put(name, value);
    }

    @Override
    public String remove(String refName) {
        checkNotNull(refName);
        String oldValue = refs.remove(refName);
        return oldValue;
    }

    @Override
    public String getSymRef(String name) {
        checkNotNull(name);
        String value = refs.get(name);
        if (value == null) {
            return null;
        }
        if (!value.startsWith("ref: ")) {
            throw new IllegalArgumentException(name + " is not a symbolic ref: '" + value + "'");
        }
        return value.substring("ref: ".length());
    }

    @Override
    public String putSymRef(String name, String val) {
        checkNotNull(name);
        checkNotNull(val);
        String old;
        try {
            old = getSymRef(name);
        } catch (IllegalArgumentException e) {
            old = null;
        }
        val = "ref: " + val;
        refs.put(name, val);
        return old;
    }

    @Override
    public Map<String, String> getAll() {

        Predicate<String> keyPredicate = new Predicate<String>() {

            @Override
            public boolean apply(String refName) {
                return refName.startsWith("refs/");
            }
        };
        return Maps.filterKeys(ImmutableMap.copyOf(this.refs), keyPredicate);
    }

}
