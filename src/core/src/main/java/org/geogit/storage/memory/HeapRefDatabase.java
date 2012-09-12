/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.memory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject.TYPE;
import org.geogit.storage.RefDatabase;

import com.google.common.base.Preconditions;
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

            final String headRefName = Ref.HEAD;
            condCreate(headRefName, TYPE.COMMIT);
            final String master = Ref.MASTER;
            condCreate(master, TYPE.COMMIT);
        }
    }

    private void condCreate(final String refName, TYPE type) {
        String child = refs.get(refName);
        if (null == child) {
            putRef(refName, ObjectId.NULL.toString());
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
        return refs.get(name);
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
        Preconditions.checkArgument(value == null || value.startsWith("ref: "),
                "Not a symbolic reference: " + name);
        return value == null ? null : value.substring("ref: ".length());
    }

    @Override
    public String putSymRef(String name, String val) {
        checkNotNull(name);
        checkNotNull(val);
        val = "ref: " + val;
        return refs.put(name, val);
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
